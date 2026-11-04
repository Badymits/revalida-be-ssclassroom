package org.ssglobal.revalida.ssclassroom.dto;


import lombok.Data;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;

@Data
public class ArtifactDto {

  private SubmissionKind kind;
  private String name;

}
