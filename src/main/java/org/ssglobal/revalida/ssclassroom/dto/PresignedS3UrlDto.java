package org.ssglobal.revalida.ssclassroom.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder // for testing
@AllArgsConstructor
public class PresignedS3UrlDto {

  private String uploadUrl;
  private String s3Key;
  private String httpMethod;
  private String bucketName;
  private Integer expiresIn;

}
