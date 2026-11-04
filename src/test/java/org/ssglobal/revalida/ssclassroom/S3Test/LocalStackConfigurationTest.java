package org.ssglobal.revalida.ssclassroom.S3Test;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.ssglobal.revalida.ssclassroom.dto.PresignedUrlDto;
import org.ssglobal.revalida.ssclassroom.service.S3Service.S3Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
public class LocalStackConfigurationTest {

  @Autowired
  private S3Client s3Client;

  @Autowired
  private S3Service s3Service;

  @Autowired
  private S3Presigner s3Presigner;

  private final String testBucket = "test-bucket";

  @BeforeEach
  void setUp(){
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(testBucket).build());
    } catch (Exception e) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(testBucket).build());
    }
  }

  @Test
  void testGeneratePresignedUrlWithLocalStack() {
    // Act
    PresignedUrlDto url = s3Service.createPresignedGetUrl(testBucket, "sample-file.txt");

    // Assert
    assertNotNull(url);
    assertTrue(url.getUrl().contains(testBucket));
    assertTrue(url.getUrl().contains("sample-file.txt"));
    System.out.format("Generated LocalStack URL: %s", url);
  }
}
