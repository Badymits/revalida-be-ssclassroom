package org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission;

import lombok.Data;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;

@Data
public class TurnInSubmissionRequestDto {

  private SubmissionKind kind;
  private String s3Key;

  // these fields can be null but one
  private String artifactId;
  private String body;
  private String url;

}
