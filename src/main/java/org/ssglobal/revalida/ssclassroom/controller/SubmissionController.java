package org.ssglobal.revalida.ssclassroom.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AssignmentResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.SubmissionDetailDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Unsubmit.UnsubmitResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;
import org.ssglobal.revalida.ssclassroom.service.AssignmentService;
import org.ssglobal.revalida.ssclassroom.service.DriveSubmissionProcessService;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubmissionController {

  private final AssignmentService assignmentService;
  private final SubmissionService submissionService;
  private final DriveSubmissionProcessService driveSubmissionService;

  // test controller to check connection to frontend
  @GetMapping("/check-connection")
  public ResponseEntity<String> checkConnection(){
    return ResponseEntity.status(HttpStatusCode.valueOf(200))
            .body("Hello From Backend!");
  }


  @GetMapping("/assignments/{assignmentId}/my-submission")
  public ResponseEntity<AssignmentResponseDto> fetchAssignment(
          @PathVariable String assignmentId
  ){
    return ResponseEntity.ok(assignmentService.getById(assignmentId));
  }

  @GetMapping("/submissions/{submissionId}/my-submission")
  public ResponseEntity<SubmissionDetailDto> fetchUnGradedSubmission(
          @PathVariable String submissionId
  ){
    return ResponseEntity.ok()
            .body(submissionService
                    .fetchSubmissionBySubmissionId(submissionId));
  }

  @PostMapping("/submissions/{submissionId}/upload-url")
  public ResponseEntity<UploadSubmissionResponseDto> uploadFileWithS3Url(
          @PathVariable String submissionId,
          @RequestBody UploadSubmissionRequestDto submissionRequestDto
          ) {

    return ResponseEntity.ok()
            .body(submissionService
                    .generateS3UploadUrl(submissionId, submissionRequestDto));
  }

  @PostMapping("/submissions/{submissionId}/process-files-from-drive")
  public ResponseEntity<TurnInSubmissionResponseDto> processFileFromDrive(
          @PathVariable String submissionId,
          @RequestBody String driveUrl
  ){
    return ResponseEntity.ok()
            .body(driveSubmissionService
                    .processDriveSubmission(submissionId, driveUrl));
  }

  @PostMapping("/submissions/{submissionId}/turn-in")
  public ResponseEntity<TurnInSubmissionResponseDto> turnInSubmission(
          @PathVariable String submissionId,
          @RequestBody TurnInSubmissionRequestDto submissionRequestDto
  ) {

    return ResponseEntity.ok()
            .body(submissionService
                    .turnInSubmission(submissionId, submissionRequestDto));
  }

  @PostMapping("/submissions/{submissionId}/unsubmit")
  public ResponseEntity<UnsubmitResponseDto> unsubmitSubmission(
          @PathVariable String submissionId) {
    return ResponseEntity.ok()
            .body(submissionService.unsubmitSubmission(submissionId));
  }

}



