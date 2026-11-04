package org.ssglobal.revalida.ssclassroom.dto;


import lombok.Data;

@Data
public class UploadSubmissionRequestDto {

  private String filename;
  // type/subtype data types (ex: application/json, text/html, image/png)
  private String contentType;

  private Integer sizeBytes;

}
