package org.ssglobal.revalida.ssclassroom.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AssignmentResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.SubmissionDetailDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Unsubmit.UnsubmitResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.service.AssignmentService;
import org.ssglobal.revalida.ssclassroom.service.DriveSubmissionProcessService;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;

import java.security.Principal;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubmissionController {

  private final AssignmentService assignmentService;
  private final SubmissionService submissionService;
  private final DriveSubmissionProcessService driveSubmissionService;

  @GetMapping("/assignments/{assignmentId}/my-submission")
  public ResponseEntity<AssignmentResponseDto> fetchAssignment(
          @PathVariable String assignmentId
  ){
    return ResponseEntity.ok(assignmentService.getById(assignmentId));
  }

  @GetMapping("/submissions/{submissionId}/my-submission")
  @PreAuthorize("hasRole('learner')")
  public ResponseEntity<SubmissionDetailDto> fetchUnGradedSubmission(
          @PathVariable String submissionId){
    return ResponseEntity.ok()
            .body(submissionService
                    .fetchSubmissionBySubmissionId(submissionId));
  }

  @PostMapping("/submissions/{submissionId}/upload-url")
  @PreAuthorize("hasRole('learner')")
  public ResponseEntity<UploadSubmissionResponseDto> uploadFileWithS3Url(
          @PathVariable String submissionId,
          @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
          @RequestBody UploadSubmissionRequestDto submissionRequestDto,
          Principal principal) {
    return ResponseEntity.ok()
            .body(submissionService
                    .generateS3UploadUrl(submissionId,
                            principal.getName(),
                            submissionRequestDto,
                            idempotencyKey));
  }

  @PostMapping("/submissions/{submissionId}/process-files-from-drive")
  @PreAuthorize("hasRole('learner')")
  public ResponseEntity<TurnInSubmissionResponseDto> processFileFromDrive(
          @PathVariable String submissionId,
          @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
          @RequestBody String driveUrl,
          Principal principal
  ){
    return ResponseEntity.ok()
            .body(driveSubmissionService
                    .processDriveSubmission(
                            submissionId,
                            driveUrl,
                            principal.getName(),
                            idempotencyKey
                    ));
  }

  @PostMapping("/submissions/{submissionId}/turn-in")
  @PreAuthorize("hasRole('learner')")
  public ResponseEntity<TurnInSubmissionResponseDto> turnInSubmission(
          @PathVariable String submissionId,
          @RequestHeader(value = "Idempotency-Key", required = false) String
                  idempotencyKey,
          @RequestBody TurnInSubmissionRequestDto submissionRequestDto,
          Principal principal
  ) {

    return ResponseEntity.ok()
            .body(submissionService
                    .turnInSubmission(
                            submissionId,
                            principal.getName(),
                            idempotencyKey,
                            submissionRequestDto)
            );
  }

  @PostMapping("/submissions/{submissionId}/unsubmit")
  @PreAuthorize("hasRole('learner')")
  public ResponseEntity<UnsubmitResponseDto> unsubmitSubmission(
          @PathVariable String submissionId, Principal principal) {
    return ResponseEntity.ok()
            .body(submissionService.unsubmitSubmission(submissionId, principal.getName()));
  }

}



