package org.ssglobal.revalida.ssclassroom.service;


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

@Slf4j
@Service
@RequiredArgsConstructor
public class DriveSubmissionProcessService {

  private final GoogleDriveService googleDriveService;
  private final SubmissionService submissionService;

  @Transactional
  public TurnInSubmissionResponseDto processDriveSubmission(
          String submissionId, String driveUrl){

    log.info("Extracting file from drive for submission: {}", submissionId);

    UploadSubmissionRequestDto extractedFile = googleDriveService.extractFile(driveUrl);
    UploadSubmissionResponseDto generatedPutUrl =
            submissionService.generateS3UploadUrl(submissionId, extractedFile);

    TurnInSubmissionRequestDto dtoRequest = new TurnInSubmissionRequestDto();
    dtoRequest.setKind(SubmissionKind.file);
    dtoRequest.setArtifactId(generatedPutUrl.getArtifactId());
    dtoRequest.setS3Key(generatedPutUrl.getS3Key());

    return submissionService.turnInSubmission(submissionId, dtoRequest);
  }
}
