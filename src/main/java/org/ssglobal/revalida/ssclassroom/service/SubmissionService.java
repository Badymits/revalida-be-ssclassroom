package org.ssglobal.revalida.ssclassroom.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Unsubmit.UnsubmitResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.Status;
import org.ssglobal.revalida.ssclassroom.exceptions.InvalidFileException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.exceptions.PayloadTooLargeException;
import org.ssglobal.revalida.ssclassroom.exceptions.StateInvalidException;
import org.ssglobal.revalida.ssclassroom.model.SubmissionArtifactModel;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionArtifactRepository;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionRepository;
import org.ssglobal.revalida.ssclassroom.service.S3Service.S3Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionService {

  private final SubmissionRepository submissionRepository;
  private final SubmissionArtifactRepository submissionArtifactRepository;
  private final S3Service s3Service;

  private final int MAX_FILE_SIZE = 52428800;

  // lookup submissions by assignmentId
  @Transactional(readOnly = true)
  public List<SubmissionModel> fetchSubmissionsByAssignmentId(String assignmentId){
    return submissionRepository.findByAssignmentModel_AssignmentId(assignmentId)
            .orElseThrow(() -> new EntityNotFoundException(""));
  }

  @Transactional(readOnly = true)
  public Boolean fetchSubmissionBySubmissionId(){

    return false;
  }

  @Transactional
  public UploadSubmissionResponseDto generateUploadUrlSubmission(
          UploadSubmissionRequestDto submissionRequestDto) {

    log.info("Generate Upload URL Service");

    if (submissionRequestDto.getSizeBytes() > MAX_FILE_SIZE){
      log.error("File size is too large! Submitted file size: {} MB",
              submissionRequestDto.getSizeBytes() / (1024.0 * 1024.0));

      throw new PayloadTooLargeException("File is too large!");
    }

    // get the value after the regex which is dot
    String fileType = retrieveFileType(submissionRequestDto.getFilename());

    // Retrieve URL
    PresignedS3UrlDto generatedPutUrl = s3Service.createPresignedPutUrl(
              submissionRequestDto.getFilename(),
              submissionRequestDto.getContentType(),
              fileType
            );

    // Create artifact which will return artifact ID back to the frontend
    SubmissionArtifactModel artifactModel = new SubmissionArtifactModel();
    artifactModel.setUrl(generatedPutUrl.getUrl());

    submissionArtifactRepository.save(artifactModel);

    return toDto(artifactModel.getArtifactId(),
            generatedPutUrl.getS3Key(),
            generatedPutUrl
    );
  }

  @Transactional
  public TurnInSubmissionResponseDto updateSubmissionTurnIn(
          String submissionId,
          TurnInSubmissionRequestDto submissionRequestDto
  ){

    log.info("Updating Submission artifact to Submitted");

    if (submissionRequestDto.getArtifactId().isEmpty()){
      log.error("Artifact ID is null!");
      throw NotFoundException.of("Submission Artifact",
              submissionRequestDto.getArtifactId());
    }

    if (submissionRequestDto.getKind() == null){
      log.error("Submission with null kind is not allowed");
      throw new StateInvalidException("Current state is not allowed!");
    }

    SubmissionArtifactModel artifactModel = submissionArtifactRepository
            .findByArtifactId(submissionRequestDto.getArtifactId())
            .orElseThrow(() -> NotFoundException.of("Artifact",
                    submissionRequestDto.getArtifactId()));

    SubmissionModel submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> {
              log.error("Submission not found with ID {}", submissionId);
              return NotFoundException.of("Submission", submissionId);
            });

    submission.setStatus(Status.SUBMITTED);
    artifactModel.setSubmissionId(submissionId);
    //artifactModel.setContentType();

    submissionRepository.save(submission);
    log.info("Successfully updated submission status to SUBMITTED!");

    return null;
  }

  @Transactional
  public UnsubmitResponseDto unsubmitSubmission(String submissionId){

    log.info("Unsubmit method service starting..");

    if (submissionId.isEmpty()){
      log.error("Submission not found with: {}", submissionId);
      throw NotFoundException.of("Submission", submissionId);
    }

    SubmissionModel submission = submissionRepository.findById(submissionId)
      .orElseThrow(() -> {
        log.error("Submission not found with ID {}", submissionId);
        return NotFoundException.of("Submission", submissionId);
      });

    // cannot proceed with updating status if status is already set to assigned
    // or if submission is already graded (has returned status)
    if (submission.getStatus().equals(Status.ASSIGNED) ||
          submission.getStatus().equals(Status.RETURNED) ||
            submission.getStatus().equals(Status.INVALID)){
      log.error("Submission with state: {} is invalid", submission.getStatus());
      throw new StateInvalidException("");
    }

    submission.setStatus(Status.ASSIGNED);
    submissionRepository.save(submission);
    log.info("Successfully reverted submission back to {} status!",
            submission.getStatus());

    return toUnsubmitDto(submissionId, submission.getStatus());

  }

  private UploadSubmissionResponseDto toDto(
          String artifactId, String s3Key, PresignedS3UrlDto generatedUrl){

    UploadSubmissionResponseDto responseDto = new UploadSubmissionResponseDto();

    responseDto.setArtifactId(artifactId);
    responseDto.setS3Key(s3Key);
    responseDto.setSignedUrl(generatedUrl);
    responseDto.setMessage("Generated Artifact ID");

    return responseDto;

  }

  private UnsubmitResponseDto toUnsubmitDto(String submissionId, Status status){

    UnsubmitResponseDto unsubmitDto = new UnsubmitResponseDto();

    unsubmitDto.setSubmissionId(submissionId);
    unsubmitDto.setStatus(status);
    unsubmitDto.setSubmittedAt(
            LocalDateTime.now()
    );

    return unsubmitDto;

  }

  private String retrieveFileType(String filename) {
    int lastIndex = filename.lastIndexOf('.');

    if (lastIndex != -1 && lastIndex < filename.length() - 1){
      return filename.substring(lastIndex + 1).toLowerCase();
    } else {
      throw new InvalidFileException("File type of ");
    }
  }
}
