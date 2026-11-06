package org.ssglobal.revalida.ssclassroom.dto;

import java.time.Instant;

import org.ssglobal.revalida.ssclassroom.model.GradeModel;

public record ReturnResponseDto(String id, String status, GradeDto grade) {

	public record GradeDto(
			Integer points, 
			String privateComment, 
			Instant returnedAt, 
			String returnedBy) {
	}

	public static ReturnResponseDto from(GradeModel grade) {
		GradeDto gradeDto = new GradeDto(
				grade.getPoints(), 
				grade.getPrivateComment(), 
				grade.getReturnedAt(),
				grade.getReturnedBy());

		return new ReturnResponseDto(grade.getSubmissionId(), "returned", gradeDto);
	}
}