package org.ssglobal.revalida.ssclassroom.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.encrypt.AesGcmBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.ssglobal.revalida.ssclassroom.error.RestAccessDeniedHandler;
import org.ssglobal.revalida.ssclassroom.error.RestAuthenticationEntryPoint;
import org.ssglobal.revalida.ssclassroom.repository.AppUserRepository;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class MySecurityContext {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)

				.cors(Customizer.withDefaults())

				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**", "/api/v1/check-connection", "/error",
								"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
						.permitAll()

						// trainer only
						.requestMatchers(HttpMethod.POST, "/api/v1/cohorts/*/assignments").hasRole("trainer")
						.requestMatchers(HttpMethod.PATCH, "/api/v1/assignments/*").hasRole("trainer")
						.requestMatchers(HttpMethod.POST, "/api/v1/assignments/*/publish").hasRole("trainer")
						.requestMatchers(HttpMethod.GET, "/api/v1/assignments/*/submissions").hasRole("trainer")
						.requestMatchers(HttpMethod.POST, "/api/v1/submissions/*/return").hasRole("trainer")

						// learner only
						.requestMatchers(HttpMethod.GET, "/api/v1/assignments/*/my-submission").hasRole("learner")
						.requestMatchers(HttpMethod.POST, "/api/v1/submissions/*/upload-url").hasRole("learner")
						.requestMatchers(HttpMethod.POST, "/api/v1/submissions/*/turn-in").hasRole("learner")
						.requestMatchers(HttpMethod.POST, "/api/v1/submissions/*/unsubmit").hasRole("learner")

						.anyRequest().authenticated())

				// token instead of the reference projects' formLogin(): the API spec requires a Bearer JWT
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter()))
						.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler))

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));

		return http.build();
	}

	// frontend origins come from app.cors.allowed-origins (comma separated); Bearer token, so no cookies
	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// encrypts first/last name before they are stored (write-only, nothing in D4 decrypts it).
	// Spring Security AES-GCM with a random 12-byte IV per value; the key is APP_ENCRYPTION_KEY (Base64 of 16/24/32 bytes).
	@Bean
	public BytesEncryptor nameEncryptor(@Value("${APP_ENCRYPTION_KEY}") String base64Key) {
		SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
		return AesGcmBytesEncryptor.withSecretKey(key).ivGenerator(KeyGenerators.secureRandom(12)).build();
	}

	@Bean
	public UserDetailsService userDetailsService(AppUserRepository appUserRepository) {
		return username -> appUserRepository.findByUsername(username)
				.map(user -> User.withUsername(user.getUsername()).password(user.getPasswordHash())
						.roles(user.getRole().name()).build())
				.orElseThrow(() -> new UsernameNotFoundException(username));
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String secret) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(secret)));
	}

	@Bean
	public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
		return NimbusJwtDecoder.withSecretKey(secretKey(secret)).macAlgorithm(MacAlgorithm.HS256).build();
	}

	// the "role" claim ("learner" | "trainer") becomes ROLE_learner / ROLE_trainer
	private JwtAuthenticationConverter roleConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("role");
		authorities.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	private SecretKey secretKey(String secret) {
		return new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256");
	}

}
