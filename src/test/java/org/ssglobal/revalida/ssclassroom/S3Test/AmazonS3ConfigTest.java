package org.ssglobal.revalida.ssclassroom.S3Test;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;
import org.ssglobal.revalida.ssclassroom.service.S3Bucket.S3Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class AmazonS3ConfigTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private S3Presigner s3Presigner;

  @Mock
  private PresignedGetObjectRequest presignedGetObjectRequest;

  @InjectMocks
  private S3Service s3Service;

  @Test
  public void testCreatePreSignedUrlSuccess() throws Exception {
    // Arrange
    Instant mockExpiration = Instant.now().plusSeconds(3600);
    URL mockUrl = URI.create("https://s3.amazonaws.com/test-bucket/test-file.txt").toURL();

    when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenReturn(HeadBucketResponse.builder().build());

    when(presignedGetObjectRequest.url()).thenReturn(mockUrl);
    when(presignedGetObjectRequest.expiration()).thenReturn(mockExpiration);
    when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
            .thenReturn(presignedGetObjectRequest);

    // Act
    PresignedS3UrlDto generatedUrl = s3Service.createPresignedGetUrl(
            "test-file.txt", "books/sample-book.pdf");

    // Assert
    assertNotNull(generatedUrl);
    assertTrue(generatedUrl.getUploadUrl().contains("test-file.txt"));
  }
}
