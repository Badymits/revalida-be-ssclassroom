package org.ssglobal.revalida.ssclassroom.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssglobal.revalida.ssclassroom.dto.LearnerHome.LearnerHomeResponseDto;
import org.ssglobal.revalida.ssclassroom.service.LearnerHomeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LearnerHomeController {

    private final LearnerHomeService learnerHomeService;

    @GetMapping("/me/assignments")
    @PreAuthorize("hasRole('learner')")
    public ResponseEntity<List<LearnerHomeResponseDto>> myAssignments(Principal principal) {
        return ResponseEntity.ok(learnerHomeService.listMyAssignments(principal.getName()));
    }
}
