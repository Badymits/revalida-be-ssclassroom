package org.ssglobal.revalida.ssclassroom.dto.UploadSubmission;


import lombok.Data;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionType;

@Data
public class UploadSubmissionRequestDto {

  private String filename;
  // type/subtype data types (ex: application/json, text/html, application/x-zip-compressed)
  private String contentType;

  private SubmissionType kind;
  private Long sizeBytes;
  private String driveUrl;

}
