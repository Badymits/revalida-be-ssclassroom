package org.ssglobal.revalida.ssclassroom.dto;

import lombok.Data;

@Data
public class UploadSubmissionResponseDto {

  private String artifactId;
  private String s3Key;
  private PresignedUrlDto signedUrl;
  private String message;

}
