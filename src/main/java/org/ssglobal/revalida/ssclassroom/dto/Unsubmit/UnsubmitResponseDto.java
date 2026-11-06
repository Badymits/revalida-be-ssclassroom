package org.ssglobal.revalida.ssclassroom.dto.Unsubmit;


import lombok.Data;
import org.ssglobal.revalida.ssclassroom.enums.Status;


import java.time.LocalDateTime;

@Data
public class UnsubmitResponseDto {

  private String submissionId;
  private Status status;
  private LocalDateTime submittedAt;

}
