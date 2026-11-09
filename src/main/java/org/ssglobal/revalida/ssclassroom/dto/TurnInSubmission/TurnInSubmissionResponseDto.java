package org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission;


import lombok.Data;
import org.ssglobal.revalida.ssclassroom.dto.ArtifactDto;
import org.ssglobal.revalida.ssclassroom.enums.Status;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TurnInSubmissionResponseDto {

  private String submissionId;
  private Status status;
  private LocalDateTime submittedAt;
  private ArtifactDto artifact;

}
