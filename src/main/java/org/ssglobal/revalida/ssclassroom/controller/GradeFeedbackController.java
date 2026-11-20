package org.ssglobal.revalida.ssclassroom.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssglobal.revalida.ssclassroom.dto.GradeFeedback.GradeFeedbackResponseDto;
import org.ssglobal.revalida.ssclassroom.service.GradeFeedbackService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GradeFeedbackController {

    private final GradeFeedbackService gradeFeedbackService;

    @GetMapping("/me/submissions/{submissionId}/feedback")
    @PreAuthorize("hasRole('learner')")
    public ResponseEntity<GradeFeedbackResponseDto> myFeedback(@PathVariable String submissionId, Principal principal) {
        return ResponseEntity.ok(gradeFeedbackService.getMyFeedback(submissionId, principal.getName()));
    }
}
