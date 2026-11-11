package org.ssglobal.revalida.ssclassroom.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignedS3UrlDto {

  private String uploadUrl;
  private String s3Key;
  private String httpMethod;
  private String bucketName;
  private Integer expiresIn;

}
