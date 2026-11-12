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
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;
import org.ssglobal.revalida.ssclassroom.service.GoogleDrive.GoogleDriveService;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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
          String submissionId, String driveUrl){

    log.info("Extracting file from drive for submission: {}", submissionId);

    File extractedFile = googleDriveService.extractFile(driveUrl);

    UploadSubmissionResponseDto generatedPutUrl =
            submissionService.generateS3UploadUrl(
                    submissionId,
                    toUploadSubmissionRequest(extractedFile)
            );
    uploadObjectToBucket(extractedFile, generatedPutUrl);


    return submissionService.turnInSubmission(
            submissionId,
            toTurnInSubmissionRequest(generatedPutUrl)
    );
  }

  private void uploadObjectToBucket(
          File extractedFile,
          UploadSubmissionResponseDto generatedPutUrl
  ){
    log.info("Uploading file to bucket...");
    try (InputStream fileInputStream =
                 googleDriveService.downloadFileStream(extractedFile.getId())) {

      log.info("Initiating Put request...");
      s3Client.putObject(
              PutObjectRequest.builder()
                      .bucket(generatedPutUrl.getSignedUrl()
                              .getBucketName())
                      .key(generatedPutUrl.getSignedUrl()
                              .getS3Key())
                      .contentType(extractedFile.getMimeType())
                      .build(),
              RequestBody.fromInputStream(fileInputStream,
                      extractedFile.getSize())
      );
      log.info("Successfully uploaded object to bucket!");

    } catch (Exception e) {
      log.error("Failed to upload file to S3: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to upload file to S3", e);
    }

  }

  private UploadSubmissionRequestDto toUploadSubmissionRequest(File extractedFile){
    UploadSubmissionRequestDto requestDto = new UploadSubmissionRequestDto();

    requestDto.setKind(SubmissionKind.file);
    requestDto.setFilename(extractedFile.getName());
    requestDto.setContentType(extractedFile.getMimeType());
    requestDto.setSizeBytes(extractedFile.getSize());

    return requestDto;
  }

  private TurnInSubmissionRequestDto toTurnInSubmissionRequest (
          UploadSubmissionResponseDto generatedPutUrl
  ){

    TurnInSubmissionRequestDto requestDto = new TurnInSubmissionRequestDto();

    requestDto.setKind(SubmissionKind.file);
    requestDto.setS3Key(generatedPutUrl.getS3Key());
    requestDto.setArtifactId(generatedPutUrl.getArtifactId());

    return requestDto;
  }
}
