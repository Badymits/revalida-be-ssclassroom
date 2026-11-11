package org.ssglobal.revalida.ssclassroom.dto.UploadSubmission;


import lombok.Data;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;

@Data
public class UploadSubmissionRequestDto {

  private String filename;
  // type/subtype data types (ex: application/json, text/html, image/png)
  private String contentType;

  private SubmissionKind kind;
  private Long sizeBytes;

}
