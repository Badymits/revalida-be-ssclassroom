package org.ssglobal.revalida.ssclassroom.controller;

import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

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
import lombok.RequiredArgsConstructor;

import org.ssglobal.revalida.ssclassroom.dto.Publish.*;
import org.ssglobal.revalida.ssclassroom.service.AssignmentService;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.*;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContentController {

    private final AssignmentService assignmentService;

    @PostMapping("/cohorts/{cohortId}/assignments")
    public ResponseEntity<AssignmentResponseDto> create(
            @PathVariable String cohortId,
            @Valid @RequestBody AssignmentCreateRequestDto request) {
        AssignmentResponseDto created = assignmentService.create(cohortId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/assignments/" + created.getId()))
                .body(created);
    }

    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<AssignmentResponseDto> get(@PathVariable String assignmentId) {
        return ResponseEntity.ok(assignmentService.getById(assignmentId));
    }

    @GetMapping("/cohorts/{cohortId}/assignments")
    public ResponseEntity<List<AssignmentResponseDto>> listByCohort(@PathVariable String cohortId) {
        return ResponseEntity.ok(assignmentService.getByCohort(cohortId));
    }

    @PatchMapping("/assignments/{assignmentId}")
    public ResponseEntity<AssignmentResponseDto> update(
            @PathVariable String assignmentId,
            @Valid @RequestBody AssignmentUpdateRequestDto request) {
        return ResponseEntity.ok(assignmentService.update(assignmentId, request));
    }

    @PostMapping("/assignments/{assignmentId}/publish")
    public ResponseEntity<PublishResponseDto> publish(
            @PathVariable String assignmentId,
            @Valid @RequestBody PublishRequestDto request) {
        return ResponseEntity.ok(assignmentService.publish(assignmentId, request));
    }
}