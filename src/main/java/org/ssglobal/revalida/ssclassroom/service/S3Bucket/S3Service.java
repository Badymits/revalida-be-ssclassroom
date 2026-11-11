// https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html
// reference to work with Amazon s3 pre-signed URLs

package org.ssglobal.revalida.ssclassroom.service.S3Bucket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;
import org.ssglobal.revalida.ssclassroom.exceptions.BucketNotFoundException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;


@Service
@Slf4j
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.buckets.pdf}")
    private String awsPdfBucket;

    @Value("${aws.s3.buckets.txt}")
    private String awsTxtBucket;

    @Value("${aws.s3.buckets.zip}")
    private String awsZipBucket;

    @Value("${aws.s3.buckets.docx}")
    private String awsDocxBucket;

    @Autowired
    public S3Service(S3Presigner s3Presigner, S3Client s3Client){
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
    }

    //  creates time-limited URLs that grant temporary access to
    //  specific S3 objects without requiring AWS credentials, ideal for secure file sharing.
    public PresignedS3UrlDto createPresignedGetUrl(String bucketName, String keyName){

      log.info("Generating presigned GET url for {}", keyName);
        try {

          if (bucketName == null || bucketName.isEmpty()) {
              log.error("Failed to provide bucket name for presigned GET request!");
              throw new BucketNotFoundException("Bucket name cannot be null or empty.");
          }

          if (keyName == null || keyName.isEmpty()){
              log.error("Failed to provide S3 Key for presigned GET request!");
              throw new Exception("Key for S3 Object cannot be empty!");
          }

          // HeadBucket check to verify if bucket exists in S3
          HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                  .bucket(bucketName)
                  .build();
          s3Client.headBucket(headBucketRequest);


          GetObjectRequest objectRequest = GetObjectRequest.builder()
                  .bucket(bucketName)
                  .key(keyName)
                  .build();

          GetObjectPresignRequest presignRequest = GetObjectPresignRequest
                  .builder()
                  .signatureDuration(Duration.ofMinutes(10))
                  .getObjectRequest(objectRequest)
                  .build();

          PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner
                  .presignGetObject(presignRequest);

          Integer expirationEpoch =
                  (int) presignedGetObjectRequest.expiration().getEpochSecond();

          log.info("Successfully generated GET request URL");
          return new PresignedS3UrlDto(
                  presignedGetObjectRequest.url().toExternalForm(),
                  "placeholder_S3Key",
                  "GET",
                  bucketName,
                  expirationEpoch
          );

        } catch (NoSuchBucketException ex) {
            log.error("Bucket with name: {} does not exist", bucketName);
            throw ex;
        } catch (Exception e) {
          log.error("Exception: {}", e.getMessage());
          throw new RuntimeException(e);
        }
    }

    public PresignedS3UrlDto createPresignedPutUrl (
            String originalFileName, String contentType, String fileType){
      log.info("Generating presigned PUT url for key");

      // must add verifier for acceptable content types
      try {

        String s3Key = generatedUniqueKey(originalFileName);
        String bucketName = resolveBucketForFile((fileType));

        if (bucketName == null || bucketName.isEmpty()){
            throw new BucketNotFoundException("Bucket for file type '" +
                    fileType + "' not found or not configured.");
        }

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType) // Crucial so S3 stores the correct MIME type
                .build();

        log.info("Created PutObjectRequest! {}", objectRequest.toString());

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest =
                s3Presigner.presignPutObject(presignRequest);

        Integer expirationEpoch =
                (int) presignedPutObjectRequest.expiration().getEpochSecond();

        log.info("Successfully generated PUT URL for key: {}", s3Key);

        return new PresignedS3UrlDto(
            presignedPutObjectRequest.url().toExternalForm(),
            s3Key,
            "PUT",
                bucketName,
            expirationEpoch
        );

      } catch (BucketNotFoundException e) {
          log.error("Bucket Not Found: {}", e.getMessage());
          throw e;
      } catch (Exception e) {
        log.error("Exception Occurred: {}", e.getMessage());
        throw new RuntimeException(e);
      }
    }

    public void deleteFileFromBucket(String bucketName, String s3Key){
      log.info("Deleting Object from s3 bucket {} with key {}", bucketName, s3Key);
      try {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("Successfully deleted object with key {}", s3Key);

      } catch (S3Exception ex){
        log.error("Failed to delete file from bucket {} with key {}", bucketName, s3Key);
        throw new RuntimeException("Failed to delete file");
      }
    }

    private String generatedUniqueKey(String originalFilename){

      return String.format(
              "sub/%s/%s",
              UUID.randomUUID().toString(),
              originalFilename);
    }

    public String resolveBucketForFile(String fileExtension) {
      return switch (fileExtension.toLowerCase()){
        case "pdf" -> awsPdfBucket;
        case "txt" -> awsTxtBucket;
        case "zip" -> awsZipBucket;
        case "docs", "docx" -> awsDocxBucket;
        default -> {
            log.error("File type received: {}", fileExtension);
            throw new BucketNotFoundException(("Unsupported File Type. " +
                    "Bucket Does not exist for file type %s")
                .formatted(fileExtension));
        }
      };
    }
}
