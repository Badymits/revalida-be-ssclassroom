package org.ssglobal.revalida.ssclassroom.S3Test;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;
import org.ssglobal.revalida.ssclassroom.exceptions.BucketNotFoundException;
import org.ssglobal.revalida.ssclassroom.service.S3Service.S3Service;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("local")
public class LocalStackConfigurationTest {

  @Autowired
  private S3Client s3Client;

  @Autowired
  private SubmissionService submissionService;

  @Autowired
  private S3Service s3Service;

  @Value("${aws.s3.buckets.txt}")
  private String txtBucketName;

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
  @Disabled
  void testGeneratePresignedUrlWithLocalStack() {
    // Act
    PresignedS3UrlDto url = s3Service.createPresignedGetUrl(testBucket, "sample-file.txt");

    // Assert
    assertNotNull(url);
    assertTrue(url.getUrl().contains(testBucket));
    assertTrue(url.getUrl().contains("sample-file.txt"));
    System.out.format("Generated LocalStack URL: %s", url);
  }

  @Test
  @Disabled
  public void testPresignedPutUrlDurationExceed15Minutes() {

    PresignedS3UrlDto url = s3Service.createPresignedPutUrl(
            "", "", "");
  }

  @Test
  public void testUploadObjectsToS3() throws IOException, InterruptedException {

    File dummyFile = new ClassPathResource("dummytextfiles/DummyTxt1.txt").getFile();
    String originalFilename = dummyFile.getName();

    PresignedS3UrlDto putUrlDto = s3Service.createPresignedPutUrl(
            dummyFile.getName(),
            "application/text",
            submissionService.retrieveFileExtension(originalFilename)
    );

    assertNotNull(putUrlDto);
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpRequest uploadObjectRequest = HttpRequest.newBuilder()
            .uri(URI.create(putUrlDto.getUrl()))
            .header("Content-type", "text/plain")
            .PUT(HttpRequest.BodyPublishers.ofFile(dummyFile.toPath()))
            .build();

    HttpResponse<Void> uploadResponse = httpClient.send(uploadObjectRequest,
            HttpResponse.BodyHandlers.discarding());

    assertEquals(200, uploadResponse.statusCode(),
            "File Upload to S3 has failed");

    submissionService.updateSubmissionTurnIn();

    HeadObjectRequest headRequest = HeadObjectRequest.builder()
            .bucket(putUrlDto.getBucketName())
            .key(putUrlDto.getS3Key())
            .build();

    HeadObjectResponse headResponse = s3Client.headObject(headRequest);

    assertNotNull(headResponse,
            "Object metadata should exist in LocalStack S3");

    assertEquals(dummyFile.length(), headResponse.contentLength(),
            "Upload file size should match");

  }

  @Test
  public void testGetObjectFromBucket() throws IOException, InterruptedException{

    // the key here depends on the key generated with the method above
    // as well as the key saved in the database
    String s3Key = "sub/d0f44d50-124d-4462-a95e-dcef7ae654ea/DummyTxt1.txt";

    // Step 1: Call your service method to generate the GET pre-signed URL DTO
    PresignedS3UrlDto getUrlDto = s3Service.createPresignedGetUrl(txtBucketName, s3Key);

    assertNotNull(getUrlDto, "Presigned S3 URL DTO should not be null");

    assertNotNull(getUrlDto.getUrl(), "URL string should not be null");
    assertEquals("GET", getUrlDto.getHttpMethod(), "HTTP method should be GET");

    // Step 2: Use Java's HttpClient to test downloading the file using the generated GET URL
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(getUrlDto.getUrl()))
            .GET()
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    // Step 3: Assert that the request succeeded and returned the correct file contents
    assertEquals(200, response.statusCode(), "GET request to pre-signed URL should return 200 OK");
    assertFalse(response.body().isEmpty(), "Downloaded body should not be empty");

    System.out.println("Successfully retrieved file content via GET URL: " + response.body());
  }

  @Test
  public void testBucketNotFoundExceptionIsThrown() {
    String nonExistentBucket = "non-existent-bucket-xyz-123";
    String s3Key = "sub/d0f44d50-124d-4462-a95e-dcef7ae654ea/DummyTxt1.txt";

    // Verify that calling service method with an invalid bucket
    // throws the custom exception
    assertThrows(
            BucketNotFoundException.class,
            () -> s3Service.createPresignedGetUrl(nonExistentBucket, s3Key),
            "Expected createPresignedGetUrl to throw BucketNotFoundException"
    );
  }
}
