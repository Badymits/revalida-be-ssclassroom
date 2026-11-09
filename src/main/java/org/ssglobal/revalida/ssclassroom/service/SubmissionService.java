package org.ssglobal.revalida.ssclassroom.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.ArtifactDto;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Unsubmit.UnsubmitResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.Status;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;
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
          String submissionId,
          UploadSubmissionRequestDto submissionRequestDto) {

    log.info("Generate PUT method presigned URL service...");
    log.info(submissionRequestDto.toString());

    if (submissionRequestDto.getSizeBytes() > MAX_FILE_SIZE){
      log.error("File size is too large! Submitted file size: {} MB",
              submissionRequestDto.getSizeBytes() / (1024.0 * 1024.0));

      throw new PayloadTooLargeException("File is too large!");
    }

    if (submissionRequestDto.getFilename() == null ||
          submissionRequestDto.getFilename().isEmpty()
    ){
      log.error("Filename is null! Cannot proceed with upload to S3 bucket!");
      throw new NotFoundException("Filename is null! Cannot proceed with upload to S3 bucket!");
    }

    // add checker to verify if content type is MIME (type/subtype) appropriate

    // get the value after the regex which is dot
    String fileExtension =
            retrieveFileExtension(submissionRequestDto.getFilename());

    // Retrieve URL
    PresignedS3UrlDto generatedPutUrl = s3Service.createPresignedPutUrl(
            submissionRequestDto.getFilename(),
            submissionRequestDto.getContentType(),
            fileExtension
          );

    // Create artifact which will return artifact ID back to the frontend
    SubmissionArtifactModel artifactModel = new SubmissionArtifactModel();

    artifactModel.setSubmissionId(submissionId);
    artifactModel.setKind(submissionRequestDto.getKind());

    artifactModel.setArtifactName(submissionRequestDto.getFilename());
    artifactModel.setContentType(submissionRequestDto.getContentType());
    artifactModel.setSizeBytes(submissionRequestDto.getSizeBytes());
    artifactModel.setS3Key(generatedPutUrl.getS3Key());


    submissionArtifactRepository.save(artifactModel);

    return toUploadSubmissionResponseDto(
            artifactModel.getArtifactId(),
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

    if (submissionRequestDto.getKind() == null){
      log.error("Submission with null kind is not allowed");
      throw new StateInvalidException("Current state is not allowed!");
    }

    SubmissionModel submission = submissionRepository.findBySubmissionId(submissionId)
      .orElseThrow(() -> {
        log.error("Submission not found with ID {}", submissionId);
        return NotFoundException.of("Submission", submissionId);
      });

    SubmissionArtifactModel artifactModel = resolveOrCreateArtifact(submissionRequestDto);
    setArtifactDetails(artifactModel, submissionRequestDto, submissionId);

    // set submission details
    submission.setStatus(Status.submitted);
    submission.setSubmittedAt(LocalDateTime.now());

    submissionRepository.save(submission);
    log.info("Successfully updated submission status to SUBMITTED");

    submissionArtifactRepository.save(artifactModel);
    log.info("Successfully saved artifact model with ID {}",
            submissionRequestDto.getArtifactId());

    return toTurnInResponseDto(
            submission.getSubmissionCompositeId().getSubmissionId(),
            submission.getStatus(),
            submission.getSubmittedAt(),
            toArtifactDto(
                    artifactModel.getKind(),
                    artifactModel.getArtifactName(),
                    artifactModel.getSizeBytes()
            )
    );
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

    // cannot proceed if submission status is assigned or returned
    if (!submission.getStatus().equals(Status.submitted)){
      log.error("Submission with state: {} is invalid", submission.getStatus());
      throw new StateInvalidException("Submission with state: {} is invalid %s"
              .formatted( submission.getStatus()));
    }

    submission.setStatus(Status.assigned);
    submissionRepository.save(submission);
    log.info("Successfully reverted submission back to {} status!",
            submission.getStatus());

    return toUnsubmitDto(submissionId, submission.getStatus());

  }

  private SubmissionArtifactModel resolveOrCreateArtifact(TurnInSubmissionRequestDto dto) {

    if (dto.getKind().equals(SubmissionKind.file)) {

      if (dto.getArtifactId() == null || dto.getArtifactId().isEmpty()){
        log.error("Artifact ID is required for file submissions!");
        throw NotFoundException.of("Submission Artifact", "Missing Artifact ID");
      }

      return submissionArtifactRepository.findByArtifactId(dto.getArtifactId())
              .orElseThrow(() -> NotFoundException.of(
                      "SubmissionArtifactModel",
                      dto.getArtifactId()
              ));
    }

    // Create new artifact model for text or link submissions
    log.info("Creating new artifact ID for {} submission", dto.getKind());
    return new SubmissionArtifactModel();
  }

  private void setArtifactDetails(
          SubmissionArtifactModel artifact,
          TurnInSubmissionRequestDto dto,
          String submissionId) {

    log.info("Setting artifact details for artifact: {}", artifact.getArtifactId());
    artifact.setSubmissionId(submissionId);
    artifact.setKind(dto.getKind());

    if (dto.getKind().equals(SubmissionKind.text)) {
      log.info("Setting values for text submission");

      if (dto.getBody() == null || dto.getBody().isBlank()) {
        throw new StateInvalidException("Text submission body cannot be empty");
      }

      artifact.setBody(dto.getBody());
      artifact.setUrl(null);

    } else if (dto.getKind().equals(SubmissionKind.link)) {
      log.info("Setting values for link submission");

      if (dto.getUrl() == null || dto.getUrl().isBlank()) {
        throw new StateInvalidException("Link submission URL cannot be empty");
      }

      artifact.setUrl(dto.getUrl());
      artifact.setBody(null);
    }

    submissionArtifactRepository.save(artifact);
  }

  private UploadSubmissionResponseDto toUploadSubmissionResponseDto(
          String artifactId, String s3Key, PresignedS3UrlDto generatedUrl){

    UploadSubmissionResponseDto responseDto = new UploadSubmissionResponseDto();

    responseDto.setArtifactId(artifactId);
    responseDto.setS3Key(s3Key);
    responseDto.setSignedUrl(generatedUrl);
    responseDto.setMessage("Generated Artifact ID");

    log.info("Upload Submission Request DTO generated!");

    return responseDto;

  }

  private TurnInSubmissionResponseDto toTurnInResponseDto(
      String submissionId,
      Status status,
      LocalDateTime submittedAt,
      ArtifactDto artifact
  ){

    TurnInSubmissionResponseDto responseDto =
            new TurnInSubmissionResponseDto();

    responseDto.setArtifact(artifact);
    responseDto.setStatus(status);
    responseDto.setSubmittedAt(submittedAt);
    responseDto.setSubmissionId(submissionId);

    return responseDto;
  }

  private UnsubmitResponseDto toUnsubmitDto(String submissionId, Status status){

    UnsubmitResponseDto unsubmitDto = new UnsubmitResponseDto();

    unsubmitDto.setSubmissionId(submissionId);
    unsubmitDto.setStatus(status);
    unsubmitDto.setSubmittedAt(
            LocalDateTime.now()
    );

    log.info("Unsubmit DTO generated!");

    return unsubmitDto;

  }

  private ArtifactDto toArtifactDto(
          SubmissionKind kind,
          String filename,
          Long sizeBytes
  ){
    ArtifactDto artifactDto = new ArtifactDto();

    artifactDto.setKind(kind);
    artifactDto.setName(filename);
    artifactDto.setSizeBytes(sizeBytes);

    return artifactDto;
  }

  public String retrieveFileExtension(String filename) {
    log.info("Retrieving file extension...");
    int lastIndex = filename.lastIndexOf('.');

    if (lastIndex != -1 && lastIndex < filename.length() - 1){
      String fileExtension = filename.substring(lastIndex + 1)
              .toLowerCase();
      log.info("Successfully retrieved file extension: {}", fileExtension);

      return fileExtension;
    } else {
      log.error("Extension type not found!");
      throw new StringIndexOutOfBoundsException("Cannot read string!");
    }
  }
}
