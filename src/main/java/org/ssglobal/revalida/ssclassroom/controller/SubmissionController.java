package org.ssglobal.revalida.ssclassroom.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ssglobal.revalida.ssclassroom.dto.SubmissionDetailDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Unsubmit.UnsubmitResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;
import org.ssglobal.revalida.ssclassroom.service.DriveSubmissionProcessService;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubmissionController {

  private final SubmissionService submissionService;
  private final DriveSubmissionProcessService driveSubmissionService;

  // test controller to check connection to frontend
  @GetMapping("/check-connection")
  public ResponseEntity<String> checkConnection(){
    return ResponseEntity.status(HttpStatusCode.valueOf(200))
            .body("Hello From Backend!");
  }


  @GetMapping("/assignments/{assignmentId}/my-submission")
  public ResponseEntity<String> submitAssignment(
          @PathVariable String assignmentId,
          @RequestBody SubmissionModel submissionModel
  ){

    return ResponseEntity.ok("success");
  }

  @GetMapping("/submissions/{submissionId}/my-submission")
  public ResponseEntity<SubmissionDetailDto> fetchUnGradedSubmission(
          @PathVariable String submissionId
  ){
    SubmissionDetailDto artifact = submissionService.fetchSubmissionBySubmissionId(submissionId);
    return ResponseEntity.status(HttpStatusCode.valueOf(200))
            .body(artifact);
  }

  @PostMapping("/submissions/{submissionId}/upload-url")
  public ResponseEntity<UploadSubmissionResponseDto> uploadFileWithS3Url(
          @PathVariable String submissionId,
          @RequestBody UploadSubmissionRequestDto submissionRequestDto
          ) {
    UploadSubmissionResponseDto submissionResponseDto =
            submissionService.generateS3UploadUrl(submissionId, submissionRequestDto);

    return ResponseEntity.status(HttpStatusCode.valueOf(200))
            .body(submissionResponseDto);
  }

  @PostMapping("/submissions/{submissionId}/process-files-from-drive")
  public ResponseEntity<TurnInSubmissionResponseDto> processFileFromDrive(
          @PathVariable String submissionId,
          @RequestParam String driveUrl
  ){
    TurnInSubmissionResponseDto responseDto =
            driveSubmissionService.processDriveSubmission(submissionId, driveUrl);

    return ResponseEntity.status(HttpStatusCode.valueOf(200)).body(responseDto);
  }

  @PostMapping("/submissions/{submissionId}/turn-in")
  public ResponseEntity<TurnInSubmissionResponseDto> turnInSubmission(
          @PathVariable String submissionId,
          @RequestBody TurnInSubmissionRequestDto submissionRequestDto
  ) {
    TurnInSubmissionResponseDto responseDto =
            submissionService.turnInSubmission(submissionId, submissionRequestDto);

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

//  @GetMapping("/submissions/{submissionId}")
//  public ResponseEntity<String> fetchSubmission() {
//
//    return ResponseEntity.ok("submission id 1 retrieved");
//  }


}



