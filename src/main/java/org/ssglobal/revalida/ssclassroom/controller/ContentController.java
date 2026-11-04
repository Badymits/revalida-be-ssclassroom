package org.ssglobal.revalida.ssclassroom.controller;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.ssglobal.revalida.ssclassroom.dto.Publish.*;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.*;


@RestController
@RequestMapping("/api/v1")
public class ContentController {
 
  @PostMapping("/cohorts/{cohortId}/assignments")
  public ResponseEntity<AssignmentResponseDto> create(
      @PathVariable String cohortId,
      @Valid @RequestBody AssignmentResponseDto request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
 
  @GetMapping("/assignments/{assignmentId}")
  public ResponseEntity<AssignmentResponseDto> get(
      @PathVariable String assignmentId) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
 
  @PatchMapping("/assignments/{assignmentId}")
  public ResponseEntity<AssignmentResponseDto> update(
      @PathVariable String assignmentId,
      @Valid @RequestBody AssignmentResponseDto request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
 
  @PostMapping("/assignments/{assignmentId}/publish")
  public ResponseEntity<PublishResponseDto> publish(
      @PathVariable String assignmentId,
      @Valid @RequestBody PublishRequestDto request) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}

