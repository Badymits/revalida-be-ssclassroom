package org.ssglobal.revalida.ssclassroom.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.ArtifactDto;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;
import org.ssglobal.revalida.ssclassroom.dto.SubmissionDetailDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Unsubmit.UnsubmitResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.AllowedContentType;
import org.ssglobal.revalida.ssclassroom.enums.AllowedFileType;
import org.ssglobal.revalida.ssclassroom.enums.Status;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;
import org.ssglobal.revalida.ssclassroom.exceptions.*;
import org.ssglobal.revalida.ssclassroom.model.SubmissionArtifactModel;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionArtifactRepository;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionRepository;
import org.ssglobal.revalida.ssclassroom.service.S3Bucket.S3Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionService {

  private final SubmissionRepository submissionRepository;
  private final SubmissionArtifactRepository submissionArtifactRepository;
  private final S3Service s3Service;

  private final Long MAX_FILE_SIZE = 10485760L;

  @Transactional(readOnly = true)
  public SubmissionDetailDto fetchSubmissionBySubmissionId(String submissionId){

    log.info("Fetching submission using submission ID: {}", submissionId);

    if (submissionId == null){
      log.error("Submission ID is null!");
      throw new NotFoundException("Submisison ID is null!");
    }

    SubmissionModel submissionModel = submissionRepository
            .findBySubmissionId(submissionId)
            .orElseThrow(() -> new NotFoundException("Submission Model not found!"));


    List<SubmissionDetailDto.Artifact> artifacts = submissionArtifactRepository
            .findBySubmissionId(submissionId)
            .map(artifactModel -> {

              if (artifactModel.getKind().equals(SubmissionKind.file)){
                String bucketName = s3Service.resolveBucketForFile(
                        retrieveFileExtension(artifactModel.getArtifactName()));
                String url = s3Service.createPresignedGetUrl(
                        bucketName,
                        artifactModel.getS3Key()).getUploadUrl();

                return new SubmissionDetailDto.Artifact(
                        String.valueOf(artifactModel.getKind()),
                        artifactModel.getArtifactName(),
                        null,
                        url
                );
              }

              else {
                return new SubmissionDetailDto.Artifact(
                        String.valueOf(artifactModel.getKind()),
                        null,
                        artifactModel.getBody(),
                        null
                );
              }
            })
            .map(List::of)
            .orElse(Collections.emptyList());

    return new SubmissionDetailDto(
            submissionModel.getSubmissionCompositeId().getSubmissionId(),
            submissionModel.getAssignmentModel().getAssignmentId(),
            submissionModel.getSubmissionCompositeId().getLearnerId(),
            submissionModel.getStatus().toString(),
            submissionModel.getSubmittedAt(),
            artifacts,
            null
    );
  }

  @Transactional
  public UploadSubmissionResponseDto generateS3UploadUrl(
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
          submissionRequestDto.getFilename().isBlank()
    ){
      log.error("Filename is null! Cannot proceed with upload to S3 bucket!");
      throw new NotFoundException("Filename is null! Cannot proceed with upload to S3 bucket!");
    }

    // add checker to verify if content type is MIME (type/subtype) appropriate
    if (!AllowedContentType.contains(submissionRequestDto.getContentType())) {
      log.error("Invalid content type!");
      throw new IllegalArgumentException("Invalid content type!");
    }

    // get the value after the regex which is dot
    String fileExtension =
            retrieveFileExtension(submissionRequestDto.getFilename());
    log.info("Retrieved file extension type: {} for file: {}",
            fileExtension, submissionRequestDto.getFilename());

    // Retrieve URL
    PresignedS3UrlDto generatedPutUrl = s3Service.createPresignedPutUrl(
            submissionRequestDto.getFilename(),
            submissionRequestDto.getContentType(),
            fileExtension
          );
    log.info("Created PUT url for file");

    // Create artifact which will return artifact ID back to the frontend
    SubmissionArtifactModel artifactModel = new SubmissionArtifactModel();

    artifactModel.setSubmissionId(submissionId);
    artifactModel.setKind(submissionRequestDto.getKind());

    artifactModel.setArtifactName(submissionRequestDto.getFilename());
    artifactModel.setContentType(submissionRequestDto.getContentType());
    artifactModel.setSizeBytes(submissionRequestDto.getSizeBytes());
    artifactModel.setS3Key(generatedPutUrl.getS3Key());

    log.info("Successfully created file artifact");
    submissionArtifactRepository.save(artifactModel);

    log.info(generatedPutUrl.getUploadUrl());
    return toUploadSubmissionResponseDto(
            artifactModel.getArtifactId(),
            generatedPutUrl.getS3Key(),
            generatedPutUrl
    );
  }

  @Transactional
  public TurnInSubmissionResponseDto turnInSubmission(
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

    transitionState(submission, Status.submitted);

    SubmissionArtifactModel artifactModel = resolveOrCreateArtifact(
            submissionId,
            submissionRequestDto
    );

    setArtifactDetails(artifactModel, submissionRequestDto, submissionId);

    // set submission details
    submission.setStatus(Status.submitted);
    submission.setSubmittedAt(Instant.now());

    submissionRepository.save(submission);
    log.info("Successfully updated submission status to SUBMITTED");

    submissionArtifactRepository.save(artifactModel);
    log.info("Successfully saved artifact model with ID {}",
            submissionRequestDto.getArtifactId());

    String presignedUrl = null;

    if (artifactModel.getKind().equals(SubmissionKind.file)) {

      log.info("Generating GET url for artifact: {} to return to Frontend",
              artifactModel.getArtifactName());

      String bucketName = s3Service.resolveBucketForFile(
              retrieveFileExtension(artifactModel.getArtifactName())
      );

      presignedUrl = s3Service.createPresignedGetUrl(
              bucketName,
              artifactModel.getS3Key()
      ).getUploadUrl();
    }

    return toTurnInResponseDto(
            submission.getSubmissionCompositeId().getSubmissionId(),
            submission.getStatus(),
            submission.getSubmittedAt(),
            toArtifactDto(artifactModel, presignedUrl) // Will be the S3 URL for files, or null for text/links
    );
  }

  @Transactional
  public UnsubmitResponseDto unsubmitSubmission(String submissionId){

    log.info("Unsubmit method service starting..");

    if (submissionId.isBlank()){
      log.error("Submission not found with: {}", submissionId);
      throw NotFoundException.of("Submission", submissionId);
    }

    SubmissionModel submission = submissionRepository.findBySubmissionId(submissionId)
      .orElseThrow(() -> {
        log.error("Submission not found with ID {}", submissionId);
        return NotFoundException.of("Submission", submissionId);
      }
    );

    // cannot proceed if submission status is assigned or returned
    if (!submission.getStatus().equals(Status.submitted)){
      log.error("Submission with state: {} is invalid", submission.getStatus());
      throw new StateInvalidException("Submission with state: {} is invalid %s"
              .formatted( submission.getStatus()));
    }

    deleteSubmissionArtifact(submissionId);
    transitionState(submission, Status.assigned);

    submission.setStatus(Status.assigned);
    submission.setSubmittedAt(null);

    submissionRepository.save(submission);
    log.info("Successfully reverted submission back to {} status!",
            submission.getStatus());

    return toUnsubmitDto(submissionId, submission.getStatus());

  }

  private SubmissionArtifactModel resolveOrCreateArtifact(
          String submissionId,
          TurnInSubmissionRequestDto dto) {

    Optional<SubmissionArtifactModel> existingArtifact =
            submissionArtifactRepository.findBySubmissionId(submissionId);

    if (existingArtifact.isPresent()){
      log.info("Found existing artifact");
      return existingArtifact.get();
    }

    if (dto.getKind().equals(SubmissionKind.file)) {

      if (dto.getArtifactId() == null || dto.getArtifactId().isBlank()){
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

    SubmissionArtifactModel newArtifact = new SubmissionArtifactModel();
    newArtifact.setSubmissionId(submissionId);
    return newArtifact;
  }

  private void deleteSubmissionArtifact(String submissionId){

    submissionArtifactRepository.findBySubmissionId(submissionId).ifPresent(
            artifact -> {

      if (!artifact.getKind().equals(SubmissionKind.file)){
        // Delete the artifact database record
        submissionArtifactRepository.delete(artifact);
      } else {

        try {

          String bucketName = s3Service.resolveBucketForFile(
                  retrieveFileExtension(artifact.getArtifactName())
          );
          s3Service.deleteFileFromBucket(bucketName, artifact.getS3Key());

          log.info("Deleted artifact file from S3: {}", artifact.getS3Key());

        } catch (Exception e) {
          log.error("Failed to delete file from S3 during unsubmit: {}", e.getMessage());
          throw new RuntimeException("Failed to clean up storage", e);
        }
        // Delete the artifact database record
        submissionArtifactRepository.delete(artifact);
      }

      log.info("Deleted submission artifact record from database for ID: {}", submissionId);
    });

  }

  private void setArtifactDetails(
          SubmissionArtifactModel artifact,
          TurnInSubmissionRequestDto dto,
          String submissionId
  ) {

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

  private void transitionState(SubmissionModel submissionModel, Status targetStatus) {

    log.info("Updating status of submission to -> {}", targetStatus);
    if (!isValidTransition(submissionModel.getStatus(), targetStatus)){
      log.error("Cannot transition from {} to {}",
              submissionModel.getStatus(), targetStatus);
      throw new IllegalStateException("Invalid state transition");
    }

    log.info("Transition complete!");
    submissionModel.setStatus(targetStatus);
  }

  private UploadSubmissionResponseDto toUploadSubmissionResponseDto(
          String artifactId, String s3Key, PresignedS3UrlDto generatedPutUrl){

    UploadSubmissionResponseDto responseDto = new UploadSubmissionResponseDto();

    responseDto.setArtifactId(artifactId);
    responseDto.setS3Key(s3Key);
    responseDto.setSignedUrl(generatedPutUrl);
    responseDto.setMessage("Generated Artifact ID");

    log.info("Upload Submission Request DTO generated!");

    return responseDto;

  }

  private TurnInSubmissionResponseDto toTurnInResponseDto(
      String submissionId,
      Status status,
      Instant submittedAt,
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
          SubmissionArtifactModel artifactModel,
          String presignedUrl
  ){
    ArtifactDto artifactDto = new ArtifactDto();

    SubmissionKind kind = artifactModel.getKind();
    artifactDto.setKind(kind);

    if (kind.equals(SubmissionKind.file)){

      artifactDto.setName(artifactModel.getArtifactName());
      artifactDto.setSizeBytes(artifactModel.getSizeBytes());
      artifactDto.setDownloadUrl(presignedUrl);

    } else if (kind.equals(SubmissionKind.text)){
      artifactDto.setBody(artifactModel.getBody());
    } else if (kind.equals(SubmissionKind.link)){
      artifactDto.setUrl(artifactDto.getUrl());
    }

    return artifactDto;
  }

  public String retrieveFileExtension(String filename) {
    log.info("Retrieving file extension...");
    int lastIndex = filename.lastIndexOf('.');

    if (lastIndex != -1 && lastIndex < filename.length() - 1){
      String fileExtension = filename.substring(lastIndex + 1).toLowerCase();

      try {
        // Validate that it matches one of our AllowedFileType enums
        AllowedFileType allowedType = AllowedFileType.valueOf(fileExtension);
        log.info("Successfully retrieved and validated file extension: {}", allowedType);
        return allowedType.name().toLowerCase();

      } catch (IllegalArgumentException e) {
        log.error("Unsupported file extension: {}", fileExtension);
        throw new IllegalArgumentException("Unsupported file type: " + fileExtension);
      }

    } else {
      log.error("Extension type not found!");
      throw new StringIndexOutOfBoundsException("Invalid filename or extension not found!");
    }
  }

  private Boolean isValidTransition(Status currentStatus, Status targetStatus) {
    return switch (currentStatus){
      case submitted -> targetStatus == Status.submitted;
      case assigned -> targetStatus == Status.assigned;
      case returned -> false; // final state, cannot change
    };
  }
}
