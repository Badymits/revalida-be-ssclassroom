package org.ssglobal.revalida.ssclassroom.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;


@Configuration
@Slf4j
public class AmazonS3Config {

    @Value("${spring.cloud.aws.region.static}")
    private String awsRegion;

    @Bean
    public S3Client getAmazonClient(){
        log.info("Retrieving Amazon Client...");

        try {
            log.info("Successfully built S3 ");
            return S3Client.builder()
                    .region(Region.of(awsRegion))
//                    .credentialsProvider(StaticCredentialsProvider.create(
//                            AwsBasicCredentials.create("test-key", "test-secret")
//                    ))
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
//                    .credentialsProvider(StaticCredentialsProvider.create(
//                            AwsBasicCredentials.create("test-key", "test-secret")
//                    ))
                    .build();
        } catch (Exception ex) {
            log.info("Failed to initialize S3 Pre-signer: {}",
                    ex.getMessage());
          throw ex;
        }

    }

}
