package org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission;


import java.time.Instant;

import org.ssglobal.revalida.ssclassroom.dto.ArtifactDto;
import org.ssglobal.revalida.ssclassroom.enums.Status;

import lombok.Data;

@Data
public class TurnInSubmissionResponseDto {

  private String submissionId;
  private Status status;
  private Instant submittedAt;
  private ArtifactDto artifact;

}
