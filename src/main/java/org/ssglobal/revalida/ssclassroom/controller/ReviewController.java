package org.ssglobal.revalida.ssclassroom.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.ssglobal.revalida.ssclassroom.dto.ReturnRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.ReturnResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.RosterResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.SubmissionDetailDto;
import org.ssglobal.revalida.ssclassroom.model.GradeModel;
import org.ssglobal.revalida.ssclassroom.service.GradeService;
import org.ssglobal.revalida.ssclassroom.service.ReviewService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final GradeService gradeService;
    private final ReviewService reviewService;

    @Autowired
    public ReviewController(GradeService gradeService, ReviewService reviewService) {
        this.gradeService = gradeService;
        this.reviewService = reviewService;
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('trainer')")
    public ResponseEntity<RosterResponseDto> getRoster(
            @PathVariable String assignmentId,
            @RequestParam(required = false)
            @Pattern(regexp = "assigned|submitted|returned") String status,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(required = false) @Size(max = 256) String cursor) {

        return ResponseEntity.ok(reviewService.getRoster(assignmentId, status, limit, cursor));
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<SubmissionDetailDto> getSubmission(
            @PathVariable String submissionId,
            Authentication authentication) {

        boolean isTrainer = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_trainer".equals(authority.getAuthority()));

        return ResponseEntity.ok(
                reviewService.getSubmission(submissionId, authentication.getName(), isTrainer));
    }

    @PostMapping("/submissions/{submissionId}/return")
    @PreAuthorize("hasRole('trainer')")
    public ResponseEntity<ReturnResponseDto> returnSubmission(
            @PathVariable String submissionId,
            @Valid @RequestBody ReturnRequestDto request,
            Principal principal) {

        GradeModel grade = gradeService.returnSubmission(submissionId, request.points(),
                request.privateComment(), principal.getName());

        return ResponseEntity.ok(ReturnResponseDto.from(grade));
    }
}
