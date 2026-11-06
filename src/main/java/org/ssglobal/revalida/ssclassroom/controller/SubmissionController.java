package org.ssglobal.revalida.ssclassroom.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Unsubmit.UnsubmitResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubmissionController {

  private final SubmissionService submissionService;

  // controller stubs
  @GetMapping("/assignments/{assignmentId}/my-submission")
  public ResponseEntity<String> submitAssignemnt(
          @PathVariable String assignmentId,
          @RequestBody SubmissionModel submissionModel
  ){

    return ResponseEntity.ok("success");
  }

  @PostMapping("/submissions/{submissionId}/upload-url")
  public ResponseEntity<Boolean> uploadFileWithS3Url(
          @PathVariable String submissionId,
          @RequestBody UploadSubmissionRequestDto uploadSubmissionDto
          ) {

    return ResponseEntity.ok(true);
  }

  @PostMapping("/submissions/{submissionId}/turn-in")
  public ResponseEntity<TurnInSubmissionResponseDto> turnInSubmission(
          @PathVariable String submissionId,
          @RequestBody TurnInSubmissionRequestDto submissionRequestDto
  ) {
    TurnInSubmissionResponseDto responseDto =
            submissionService.updateSubmissionTurnIn(submissionId, submissionRequestDto);

    return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(responseDto);
  }

  @PostMapping("/submissions/{submissionId}/unsubmit")
  public ResponseEntity<UnsubmitResponseDto> unsubmitSubmission(
          @PathVariable String submissionId) {

    UnsubmitResponseDto responseDto =
            submissionService.unsubmitSubmission(submissionId);

    return ResponseEntity
            .status(HttpStatusCode.valueOf(200))
            .body(responseDto);
  }

  @GetMapping("/submissions/{submissionId}")
  public ResponseEntity<String> fetchSubmission() {

    return ResponseEntity.ok("submission id 1 retrieved");
  }


}



