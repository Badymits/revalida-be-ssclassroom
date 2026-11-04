package org.ssglobal.revalida.ssclassroom.dto.UploadSubmission;

import lombok.Data;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;

@Data
public class UploadSubmissionResponseDto {

  private String artifactId;
  private String s3Key;
  private PresignedS3UrlDto signedUrl;
  private String message;

}
