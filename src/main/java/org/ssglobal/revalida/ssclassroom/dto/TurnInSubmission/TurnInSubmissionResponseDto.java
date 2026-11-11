package org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission;


import lombok.Data;
import org.ssglobal.revalida.ssclassroom.dto.ArtifactDto;
import org.ssglobal.revalida.ssclassroom.enums.Status;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TurnInSubmissionResponseDto {

  private String submissionId;
  private Status status;
  private Instant submittedAt;
  private ArtifactDto artifact;

}
