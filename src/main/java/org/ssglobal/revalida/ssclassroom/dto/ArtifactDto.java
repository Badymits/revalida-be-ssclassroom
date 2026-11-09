package org.ssglobal.revalida.ssclassroom.dto;


import lombok.Data;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;

@Data
public class ArtifactDto {

  private SubmissionKind kind;

  // file type fields
  private String name;
  private Long sizeBytes;

  // text type field
  private String body;

  // link type field
  private String url;

}
