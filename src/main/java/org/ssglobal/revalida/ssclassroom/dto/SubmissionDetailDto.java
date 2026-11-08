package org.ssglobal.revalida.ssclassroom.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record SubmissionDetailDto(
		String id, 
		String assignmentId, 
		String learnerId,
        String status, 
        Instant submittedAt, 
        List<Artifact> artifacts,
        ReturnResponseDto.GradeDto grade) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Artifact(
    		String kind, 
    		String name, 
    		String body, 
    		String downloadUrl) {
    }
}