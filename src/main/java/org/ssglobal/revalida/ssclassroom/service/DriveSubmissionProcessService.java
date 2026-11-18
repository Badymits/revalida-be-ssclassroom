package org.ssglobal.revalida.ssclassroom.service;


import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionType;
import org.ssglobal.revalida.ssclassroom.service.GoogleDrive.GoogleDriveService;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriveSubmissionProcessService {

  private final GoogleDriveService googleDriveService;
  private final SubmissionService submissionService;
  private final S3Client s3Client;

  @Transactional
  public TurnInSubmissionResponseDto processDriveSubmission(
          String submissionId, String driveUrl, String learnerId, String idempotencyKey){

    log.info("Extracting file from drive for submission: {}", submissionId);

    File extractedFile = googleDriveService.extractFile(driveUrl);

    GoogleDriveService.DownloadedFile downloadedFile =
            googleDriveService.downloadFileStream(extractedFile.getId());
    UploadSubmissionResponseDto generatedPutUrl = null;
    try {

      generatedPutUrl = submissionService.generateS3UploadUrl(
        submissionId,
        learnerId,
        toUploadSubmissionRequest(downloadedFile, driveUrl),
        idempotencyKey);

      uploadObjectToBucket(downloadedFile, generatedPutUrl);
      return submissionService.turnInSubmission(
              submissionId,
              learnerId,
              idempotencyKey,
              toTurnInSubmissionRequest(generatedPutUrl, driveUrl));
    } catch (Exception e) {
      log.error("Database save failed after S3 upload. Rolling back (deleting) file from S3...", e);

      if (generatedPutUrl != null && generatedPutUrl.getSignedUrl() != null){
        try {
          String bucketName = generatedPutUrl.getSignedUrl().getBucketName();
          String s3Key = generatedPutUrl.getS3Key();

          // calling AWS to delete orphaned file
          s3Client.deleteObject(DeleteObjectRequest.builder()
                  .bucket(bucketName)
                  .key(s3Key)
                  .build());

          log.info("Successfully cleaned up orphaned file from S3: {}", s3Key);
        } catch (Exception s3Ex) {
          log.error("Critical: Failed to delete orphaned S3 file during rollback: {}",
                  s3Ex.getMessage(), s3Ex);
        }
      }
      // Rethrow the original exception so the transaction rolls back and the user gets their error response
      throw e;
    } finally {
      try {
        downloadedFile.inputStream().close();
      } catch (IOException ignored) {

      }
    }
  }

  private void uploadObjectToBucket(
          GoogleDriveService.DownloadedFile downloadedFile,
          UploadSubmissionResponseDto generatedPutUrl
  ){
    log.info("Uploading file to bucket...");

    try (InputStream fileInputStream = downloadedFile.inputStream()) {

      log.info("Initiating Put request...");
      s3Client.putObject(PutObjectRequest.builder()
        .bucket(generatedPutUrl.getSignedUrl().getBucketName())
        .key(generatedPutUrl.getSignedUrl().getS3Key())
        .contentType(downloadedFile.contentType())
        .contentLength(downloadedFile.contentLength()).build(),

        RequestBody.fromInputStream(fileInputStream,downloadedFile.contentLength())
      );
      log.info("Successfully uploaded object to bucket!");

    } catch (Exception e) {
      log.error("Failed to upload file to S3: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to upload file to S3", e);
    }

  }

  private UploadSubmissionRequestDto toUploadSubmissionRequest(
          GoogleDriveService.DownloadedFile extractedFile, String driveUrl){
    UploadSubmissionRequestDto requestDto = new UploadSubmissionRequestDto();

    requestDto.setKind(SubmissionType.link);
    requestDto.setFilename(extractedFile.resolvedFilename());
    requestDto.setContentType(extractedFile.contentType());
    requestDto.setSizeBytes(extractedFile.contentLength());
    requestDto.setDriveUrl(driveUrl);

    return requestDto;
  }

  private TurnInSubmissionRequestDto toTurnInSubmissionRequest (
          UploadSubmissionResponseDto generatedPutUrl,
          String driveUrl
  ){

    TurnInSubmissionRequestDto requestDto = new TurnInSubmissionRequestDto();

    requestDto.setKind(SubmissionType.link);
    requestDto.setUrl(driveUrl);
    requestDto.setS3Key(generatedPutUrl.getS3Key());
    requestDto.setArtifactId(generatedPutUrl.getArtifactId());

    return requestDto;
  }
}
