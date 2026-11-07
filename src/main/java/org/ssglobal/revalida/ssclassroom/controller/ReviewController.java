package org.ssglobal.revalida.ssclassroom.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssglobal.revalida.ssclassroom.dto.ReturnRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.ReturnResponseDto;
import org.ssglobal.revalida.ssclassroom.model.GradeModel;
import org.ssglobal.revalida.ssclassroom.service.GradeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

	private final GradeService gradeService;

	@Autowired
	public ReviewController(GradeService gradeService) {
		this.gradeService = gradeService;
	}

	@PostMapping("/submissions/{submissionId}/return")
	@PreAuthorize("hasRole('trainer')")
	public ResponseEntity<ReturnResponseDto> returnSubmission(
			@PathVariable String submissionId,
			@Valid @RequestBody ReturnRequestDto request, 
			Principal principal) {
		GradeModel grade = gradeService.returnSubmission(
				submissionId, 
				request.points(), 
				request.privateComment(),
				principal.getName());

		return ResponseEntity.ok(ReturnResponseDto.from(grade));
	}
}
