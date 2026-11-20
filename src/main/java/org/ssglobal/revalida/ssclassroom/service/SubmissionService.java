package org.ssglobal.revalida.ssclassroom.service;

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
import org.ssglobal.revalida.ssclassroom.enums.*;
import org.ssglobal.revalida.ssclassroom.exceptions.*;
import org.ssglobal.revalida.ssclassroom.model.IdempotencyCompositeModel;
import org.ssglobal.revalida.ssclassroom.model.IdempotencyKeyModel;
import org.ssglobal.revalida.ssclassroom.model.SubmissionArtifactModel;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;
import org.ssglobal.revalida.ssclassroom.repository.GradeDraftRepository;
import org.ssglobal.revalida.ssclassroom.repository.IdempotencyKeyRepository;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionArtifactRepository;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionRepository;
import org.ssglobal.revalida.ssclassroom.service.S3Bucket.S3Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionService {

  private final SubmissionRepository submissionRepository;
  private final SubmissionArtifactRepository submissionArtifactRepository;
  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final GradeDraftRepository gradeDraftRepository;

  private final S3Service s3Service;

  @Transactional(readOnly = true)
  public SubmissionDetailDto fetchSubmissionBySubmissionId(String submissionId, String learnerId){

    log.info("Fetching submission using submission ID: {}", submissionId);

    if (submissionId == null){
      log.error("Submission ID is null!");
      throw new NotFoundException("Submission ID is null!");
    }

    SubmissionModel submissionModel = submissionRepository
            .findBySubmissionId(submissionId)
            .orElseThrow(() -> new NotFoundException("Submission Model not found!"));

    validateLearnerOwnership(submissionModel, learnerId);

    List<SubmissionDetailDto.Artifact> artifacts = submissionArtifactRepository
            .findBySubmissionId(submissionId)
            .map(artifactModel -> {

              if (artifactModel.getKind().equals(SubmissionType.file)){
                String bucketName = s3Service.resolveBucketForFile(
                        retrieveFileExtension(artifactModel.getArtifactName()));
                String s3Url = s3Service.createPresignedGetUrl(
                        bucketName,
                        artifactModel.getS3Key()).getUploadUrl();

                return new SubmissionDetailDto.Artifact(
                        String.valueOf(artifactModel.getKind()),
                        artifactModel.getArtifactName(),
                        null,
                        artifactModel.getUrl(),
                        artifactModel.getSizeBytes(),
                        s3Url
                );
              }

              else {
                return new SubmissionDetailDto.Artifact(
                        String.valueOf(artifactModel.getKind()),
                        "",
                        artifactModel.getBody(),
                        artifactModel.getUrl(),
                        0L,
                        ""
                );
              }
            })
            .map(List::of)
            .orElse(Collections.emptyList());

    return new SubmissionDetailDto(
            submissionModel.getSubmissionCompositeId().getSubmissionId(),
            submissionModel.getAssignmentModel().getAssignmentId(),
            submissionModel.getSubmissionCompositeId().getLearnerId(),
            null,
            submissionModel.getStatus().toString(),
            submissionModel.getSubmittedAt(),
            artifacts,
            null,
            null
    );
  }

  @Transactional(readOnly = true)
  public String generateGetUrl(String submissionId, String learnerId){

    log.info("Generating Get URL for artifact: {}", submissionId);

    if (submissionId == null || submissionId.isBlank()){
      log.info("Missing artifact ID. Cannot proceed with generating get request");
      throw new ApiException(
              ErrorCode.NOT_FOUND,
              "Missing artifact ID. Cannot open file");
    }
    return submissionArtifactRepository
      .findBySubmissionId(submissionId)
        .map(artifactModel -> {

          SubmissionModel submission =
            submissionRepository.findBySubmissionId(artifactModel.getSubmissionId())
              .orElseThrow(() -> new ApiException(
                ErrorCode.NOT_FOUND, "Submission ID of artifact doesn't exist"));

          validateLearnerOwnership(submission, learnerId);

          String bucketName = s3Service.resolveBucketForFile(
                  retrieveFileExtension(artifactModel.getArtifactName()));

          return s3Service.createPresignedGetUrl(bucketName, artifactModel.getS3Key())
                  .getUploadUrl();

        })
        .orElseThrow(() -> new ApiException(
                ErrorCode.NOT_FOUND,
                "Artifact not found with ID: " + submissionId
        ));
  }

  @Transactional
  public UploadSubmissionResponseDto generateS3UploadUrl(String submissionId, String learnerId,
          UploadSubmissionRequestDto submissionRequestDto, String idempotencyKey) {

    log.info("Generate PUT method presigned URL service...");
    log.info("Checking Idempotency key...");
    log.info("DTO request: {}", submissionRequestDto.toString());

    // If key was already used by a completed submission previously, block it
    if (idempotencyKey != null && !idempotencyKey.isBlank()){
      idempotencyKeyRepository.findByIdemKey(idempotencyKey)
        .ifPresent(usedId -> {
          IdempotencyCompositeModel id = usedId.getIdempotencyCompositeModel();

          if (!id.getUserId().equals(learnerId) || !id.getSubmissionId().equals(submissionId)) {
            throw new StateInvalidException("This Idempotency-Key was already used for another request");
          }
          // If a previous request already finished the entire turn-in, stop users from generating new URLs
          throw new StateInvalidException("Submission has already been turned in with this Idempotency-Key");
      });
    }


    Long MAX_FILE_SIZE = 10485760L;
    if (submissionRequestDto.getSizeBytes() > MAX_FILE_SIZE){
      log.error("File size is too large! Submitted file size: {} MB",
              submissionRequestDto.getSizeBytes() / (1024.0 * 1024.0));

      throw new PayloadTooLargeException("File is too large! Maximum is only 10MB");
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

    // Search through the repository to find submission model and check if submission
    // is associated with logged in user
    submissionRepository.findBySubmissionId(submissionId).ifPresentOrElse(
      submissionModel -> validateLearnerOwnership(submissionModel, learnerId),
      () -> {
        log.error("Submission ID not found!");
        throw new NotFoundException("Submission ID not found!");
      }
    );

    boolean isGoogleDoc = submissionRequestDto.getContentType()!= null &&
            submissionRequestDto.getContentType().toLowerCase()
                    .contains("application/vnd.google-apps.document");

    String filename = submissionRequestDto.getFilename();

    if (filename == null || filename.isBlank()) {
      throw new ApiException(ErrorCode.NOT_FOUND, "Filename is required.");
    }

    // Native Google Docs default to docx without needing extension parsing
    String fileExtension = isGoogleDoc? "docx" : retrieveFileExtension(filename);
    log.info("Retrieved file extension type: {} for file: {}",
            fileExtension, submissionRequestDto.getFilename());

    // Retrieve URL
    PresignedS3UrlDto generatedPutUrl = s3Service.createPresignedPutUrl(
            submissionRequestDto.getFilename(),
            submissionRequestDto.getContentType(),
            fileExtension
          );
    log.info("Created PUT url for file");

    // Create artifact which will get artifact ID to return back to the frontend
    SubmissionArtifactModel artifactModel = new SubmissionArtifactModel();

    artifactModel.setSubmissionId(submissionId);
    artifactModel.setKind(submissionRequestDto.getKind());

    // Need to set url of link submissions for the artifact since
    // Flyway check constraint requires the drive url to be saved
    if (submissionRequestDto.getKind().equals(SubmissionType.link)){
      artifactModel.setUrl(submissionRequestDto.getDriveUrl());
    }

    artifactModel.setArtifactName(submissionRequestDto.getFilename());
    artifactModel.setContentType(submissionRequestDto.getContentType());
    artifactModel.setSizeBytes(submissionRequestDto.getSizeBytes());
    artifactModel.setS3Key(generatedPutUrl.getS3Key());

    log.info("Successfully created file artifact");
    submissionArtifactRepository.save(artifactModel);

    log.info(generatedPutUrl.getUploadUrl());
    return toUploadSubmissionResponseDto(
            Objects.toString(artifactModel.getArtifactId(), ""),
            generatedPutUrl.getS3Key(),
            generatedPutUrl
    );
  }

  @Transactional
  public TurnInSubmissionResponseDto turnInSubmission(String submissionId, String learnerId,
          String idempotencyKey, TurnInSubmissionRequestDto submissionRequestDto){

    log.info("Updating Submission artifact to Submitted");
    log.info("Submission type: {}", submissionRequestDto.getKind());

    Optional<TurnInSubmissionResponseDto> cachedSubmission =
            checkIdempotencyKey(idempotencyKey, learnerId, submissionId);

    if (cachedSubmission.isPresent()){
      return cachedSubmission.get();
    }

    if (submissionRequestDto.getKind() == null){
      log.error("Submission with null kind is not allowed");
      throw new StateInvalidException("Current state is not allowed!");
    }

    SubmissionModel submission = submissionRepository.findBySubmissionId(submissionId)
      .orElseThrow(() -> NotFoundException.of("Submission", submissionId));

    if (submission.getStatus().equals(Status.returned)){
      log.error("Cannot update a graded/returned submission!");
      throw new StateInvalidException("You cannot turn in another submission " +
              "if your submission is already graded!");
    }

    validateLearnerOwnership(submission, learnerId);

    // set submitted date before state transition
    submission.setSubmittedAt(Instant.now());
    transitionState(submission, Status.submitted);

    SubmissionArtifactModel artifactModel = resolveOrCreateArtifact(
            submissionId,
            submissionRequestDto
    );

    setArtifactDetails(artifactModel, submissionRequestDto, submissionId);

    submissionRepository.save(submission);
    log.info("Successfully updated submission status to SUBMITTED");

    submissionArtifactRepository.save(artifactModel);
    log.info("Successfully saved artifact model with ID {}",
            submissionRequestDto.getArtifactId());

    String presignedUrl = null;

    if (!artifactModel.getKind().equals(SubmissionType.text)) {

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

    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      log.info("Saving idempotency key to database...");
      IdempotencyKeyModel idempotencyRecord = new IdempotencyKeyModel();
      IdempotencyCompositeModel compositeModel = new IdempotencyCompositeModel();

      compositeModel.setIdemKey(idempotencyKey);
      compositeModel.setUserId(learnerId);
      compositeModel.setSubmissionId(submissionId);

      idempotencyRecord.setIdempotencyCompositeModel(compositeModel);
      idempotencyRecord.setCreatedAt(Instant.now());

      idempotencyKeyRepository.save(idempotencyRecord);
    }

    return toTurnInResponseDto(
            submission.getSubmissionCompositeId().getSubmissionId(),
            submission.getStatus(),
            submission.getSubmittedAt(),
            toArtifactDto(artifactModel, presignedUrl) // Will be the S3 URL for files, or null for text/links
    );
  }

  @Transactional
  public UnsubmitResponseDto unsubmitSubmission(String submissionId, String learnerId){

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
      throw new StateInvalidException("You can no longer unsubmit!. " +
              "Your submission has already been graded");
    }

    validateLearnerOwnership(submission, learnerId);
    deleteSubmissionArtifact(submissionId);
    transitionState(submission, Status.assigned);

    submission.setSubmittedAt(null);
    gradeDraftRepository.deleteById(submissionId);

    submissionRepository.save(submission);
    log.info("Successfully reverted submission back to {} status!",
            submission.getStatus());

    return toUnsubmitDto(submissionId, submission.getStatus());

  }

  private SubmissionArtifactModel resolveOrCreateArtifact(
          String submissionId, TurnInSubmissionRequestDto dto) {

    Optional<SubmissionArtifactModel> existingArtifact =
            submissionArtifactRepository.findBySubmissionId(submissionId);

    if (existingArtifact.isPresent()){
      log.info("Found existing artifact");
      return existingArtifact.get();
    }

    if (dto.getKind().equals(SubmissionType.file)) {

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

      if (!artifact.getKind().equals(SubmissionType.file)){
        submissionArtifactRepository.delete(artifact);
      } else {

        try {

          String bucketName = s3Service.resolveBucketForFile(
                  retrieveFileExtension(artifact.getArtifactName())
          );
          s3Service.deleteFileFromBucket(bucketName, artifact.getS3Key());

          log.info("Deleted artifact file from S3: {}", artifact.getS3Key());


          submissionArtifactRepository.delete(artifact);
        } catch (Exception e) {
          log.error("Failed to delete file from S3 during unsubmit: {}", e.getMessage());
          throw new RuntimeException("Failed to clean up storage", e);
        }
      }

      log.info("Deleted submission artifact record from database for ID: {}", submissionId);
    });

  }

  private void setArtifactDetails(SubmissionArtifactModel artifact,
          TurnInSubmissionRequestDto dto, String submissionId) {

    log.info("Setting artifact details for artifact: {}", artifact.getArtifactId());
    artifact.setSubmissionId(submissionId);
    artifact.setKind(dto.getKind());

    if (dto.getKind().equals(SubmissionType.text)) {
      log.info("Setting values for text submission");

      if (dto.getBody() == null || dto.getBody().isBlank()) {
        throw new StateInvalidException("Text submission body cannot be empty");
      }

      artifact.setBody(dto.getBody());
      artifact.setUrl(null);

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

    log.info("Upload Submission Request DTO generated!");

    return responseDto;

  }

  private TurnInSubmissionResponseDto toTurnInResponseDto(
      String submissionId,
      Status status,
      Instant submittedAt,
      ArtifactDto artifact){

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
            LocalDateTime.now());

    log.info("Unsubmit DTO generated!");

    return unsubmitDto;

  }

  private ArtifactDto toArtifactDto(
          SubmissionArtifactModel artifactModel,
          String presignedUrl){

    ArtifactDto artifactDto = new ArtifactDto();

    SubmissionType kind = artifactModel.getKind();
    artifactDto.setKind(kind);

    if (kind.equals(SubmissionType.file)){

      artifactDto.setName(artifactModel.getArtifactName());
      artifactDto.setSize(artifactModel.getSizeBytes());
      artifactDto.setDownloadUrl(presignedUrl);

    } else if (kind.equals(SubmissionType.text)){
      artifactDto.setBody(artifactModel.getBody());
    } else if (kind.equals(SubmissionType.link)){

      artifactDto.setName(artifactModel.getArtifactName());
      artifactDto.setSize(artifactModel.getSizeBytes());
      artifactDto.setDownloadUrl(presignedUrl);
      artifactDto.setUrl(artifactModel.getUrl());
    }

    return artifactDto;
  }

  private void validateLearnerOwnership(SubmissionModel submission, String learnerId) {
    log.info("Validating Submission ownership....");

    if (!submission.getSubmissionCompositeId().getLearnerId().equals(learnerId)){
      log.error("User with Id: {} does not match submission owner: {}",
              submission.getSubmissionCompositeId().getLearnerId(),
              learnerId
              );
      throw new ForbiddenException("Access denied. You do not own this submission");
    }
    log.info("Validation passed!");
  }

  public String retrieveFileExtension(String filename) {
    log.info("Retrieving file extension...");

    if (filename == null || filename.isBlank()){
      log.error("Filename is not provided! Cannot retrieve its file extension!");
      throw new NotFoundException("Filename is not provided! Cannot retrieve its file extension!");
    }

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
      case assigned -> targetStatus == Status.submitted;
      case submitted -> targetStatus == Status.returned || targetStatus == Status.assigned;
      case returned -> false; // final state, cannot change
    };
  }

  public Optional<TurnInSubmissionResponseDto> checkIdempotencyKey(
          String idempotencyKey, String userId, String submissionId){

    log.info("Checking Idempotency key....");
    if (idempotencyKey == null || idempotencyKey.isBlank()){
      return Optional.empty();
    }

    return idempotencyKeyRepository.findByIdemKey(idempotencyKey)
      .map(usedIdemKey -> {
        IdempotencyCompositeModel id = usedIdemKey.getIdempotencyCompositeModel();

        if (!id.getUserId().equals(userId) || !id.getSubmissionId().equals(submissionId)){
          throw new StateInvalidException("This Idempotency-Key was " +
                  "already used for another request");
        }

        SubmissionModel submission = submissionRepository.findBySubmissionId(submissionId)
          .orElseThrow(() -> new NotFoundException("Submission Not Found"));

        ArtifactDto artifact = submissionArtifactRepository
          .findBySubmissionId(submissionId)
          .map(artifactModel -> {

            String presignedGetUrl = null;

            if (artifactModel.getKind().equals(SubmissionType.file)){

              String bucketName = s3Service.resolveBucketForFile(
                      retrieveFileExtension(artifactModel.getArtifactName()));
              presignedGetUrl = s3Service
                      .createPresignedGetUrl(
                              bucketName,
                              artifactModel.getS3Key())
                      .getUploadUrl();
            }

            return toArtifactDto(artifactModel, presignedGetUrl);
          })
          .orElseThrow(() -> new NotFoundException("Artifact Not Found"));

        return toTurnInResponseDto(
                submissionId,
                submission.getStatus(),
                submission.getSubmittedAt(),
                artifact);
      });
  }
}
