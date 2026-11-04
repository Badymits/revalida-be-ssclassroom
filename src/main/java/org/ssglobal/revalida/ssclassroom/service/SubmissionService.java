package org.ssglobal.revalida.ssclassroom.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.PresignedUrlDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.model.SubmissionArtifactModel;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionArtifactRepository;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionRepository;
import org.ssglobal.revalida.ssclassroom.service.S3Service.S3Service;


@Service
@Slf4j
public class SubmissionService {

  @Autowired
  private SubmissionRepository submissionRepository;

  @Autowired
  private SubmissionArtifactRepository submissionArtifactRepository;

  @Autowired
  private S3Service s3Service;

  /*
    Please change to Assignment table later on
   */
  @Transactional
  public Boolean publishAssignment(SubmissionModel submissionModel) {
    return false;
  }

  @Transactional
  public SubmissionModel updateAssignmentStatus(){

    return null;
  }

  @Transactional
  public UploadSubmissionResponseDto uploadSubmission (
          UploadSubmissionRequestDto submissionRequestDto) {

    log.info("Generate Upload URL Service");

    if (submissionRequestDto.getFilename().isEmpty()){
      log.info("Cannot Save Empty Object");
      throw new RuntimeException("Empty name detected");
    }

    // Retrieve URL
    PresignedUrlDto generatedUrl =
            s3Service.createPresignedPutUrl(
              submissionRequestDto.getFilename(),
              submissionRequestDto.getContentType()
            );

    SubmissionArtifactModel artifactModel = new SubmissionArtifactModel();
    artifactModel.setUrl(generatedUrl.getUrl());

    submissionArtifactRepository.save(artifactModel);

    UploadSubmissionResponseDto responseDto = new UploadSubmissionResponseDto();

    responseDto.setArtifactId(artifactModel.getArtifactId());
    responseDto.setS3Key(generatedUrl.getS3Key());
    responseDto.setSignedUrl(generatedUrl);
    responseDto.setMessage("Generated Artifact ID");

    return toDto(
            artifactModel.getArtifactId(),
            generatedUrl.getS3Key(),
            generatedUrl
    );
  }

  private UploadSubmissionResponseDto toDto(
          String artifactId, String s3Key, PresignedUrlDto generatedUrl){
    UploadSubmissionResponseDto responseDto = new UploadSubmissionResponseDto();

    responseDto.setArtifactId(artifactId);
    responseDto.setS3Key(s3Key);
    responseDto.setSignedUrl(generatedUrl);
    responseDto.setMessage("Generated Artifact ID");

    return responseDto;
  }



}
