package org.ssglobal.revalida.ssclassroom.config;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;

import java.util.Map;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class CorsBucketConfigure {

  private final S3Client s3Client;
  private final S3PropertiesConfig s3Properties;

  @PostConstruct
  public void configureCorsForBuckets(){

    log.info("Applying CORS for each bucket...");

    CORSRule corsRule = CORSRule.builder()
            .allowedOrigins("http://localhost:3000", "https://app.localstack.cloud")
            .allowedMethods("GET", "PUT", "POST", "DELETE", "HEAD")
            .allowedHeaders("*")
            .maxAgeSeconds(3000)
            .build();

    CORSConfiguration corsConfiguration = CORSConfiguration.builder()
            .corsRules(corsRule)
            .build();

    for (Map.Entry<String, String> entry : s3Properties.getBuckets().entrySet()) {
      String bucketName = entry.getValue();
      log.info("Applying CORS policy to bucket: {}", bucketName);
      try {
        PutBucketCorsRequest corsRequest = PutBucketCorsRequest.builder()
                .bucket(bucketName)
                .corsConfiguration(corsConfiguration)
                .build();

        s3Client.putBucketCors(corsRequest);
        log.info("Successfully applied CORS policy to S3 bucket [{}] ({})", entry.getKey(), bucketName);
      } catch (Exception e) {
        log.error("Failed to configure S3 bucket CORS for {}: {}", bucketName, e.getMessage());
      }
    }
  }
}
