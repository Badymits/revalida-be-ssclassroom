package org.ssglobal.revalida.ssclassroom.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;


@Configuration
@Slf4j
public class AmazonS3Config {

    @Value("${spring.cloud.aws.region.static}")
    private String awsRegion;

    @Value("${aws.s3.endpoint}")
    private String localStackURI;

    @Bean
    public S3Client getAmazonClient(){
        log.info("Retrieving Amazon Client...");

        try {
            log.info("Successfully built S3 ");
            return S3Client.builder()
                    .region(Region.of(awsRegion))
                    .endpointOverride(URI.create(localStackURI))

                    /*
                        added credentials provider for SDK client
                        it only requires the "shape" of AWS credentials (access key and secret access key)
                        to construct and sign requests
                     */
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test-key", "test-secret")
                    ))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();
        } catch (Exception ex) {
            log.error("Failed to build Amazon Client: {}",
                    ex.getMessage());
            throw ex; // will replace this with Custom Exception
        }

    }

    @Bean
    public S3Presigner s3Presigner(){

        log.info("Initializing AWS S3 Pre-signer bean");
        try {
            log.info("S3 Pre-signer Successfully created!");
            return S3Presigner.builder()
                    .region(Region.of(awsRegion))
                    /*
                        mechanism that redirects the AWS SDK client away from
                        public AWS servers and forces it to talk to LocalStack container
                        instead.
                     */
                    .endpointOverride(URI.create(localStackURI))

                    /*
                    // LocalStack relies heavily on path-style access.
                    // Enabling this ensures SDK talks directly to localstack
                    // using simple port routing without failing DNS resolution

                     */
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test-key", "test-secret")
                    ))

                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();
        } catch (Exception ex) {
            log.info("Failed to initialize S3 Pre-signer: {}",
                    ex.getMessage());
          throw ex;
        }

    }

}
