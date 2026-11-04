package org.ssglobal.revalida.ssclassroom.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignedUrlDto {

  private String url;
  private String s3Key;
  private String method;
  private Integer expiresIn;

}
