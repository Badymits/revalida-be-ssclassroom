package org.ssglobal.revalida.ssclassroom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;


@RestController("/api/v1")
public class SubmissionController {

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
  public ResponseEntity<Boolean> turnInSubmission() {

    return ResponseEntity.ok(true);
  }

  @PostMapping("/submissions/{submissionId}/unsubmit")
  public ResponseEntity<Boolean> unsubmitSubmission() {

    return ResponseEntity.ok(true);
  }

  @GetMapping("/submissions/{submissionId}")
  public ResponseEntity<String> fetchSubmission() {

    return ResponseEntity.ok("submission id 1 retrieved");
  }


}



