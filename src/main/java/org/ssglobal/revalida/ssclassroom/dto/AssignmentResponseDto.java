package org.ssglobal.revalida.ssclassroom.dto;

import org.ssglobal.revalida.ssclassroom.enums.AssignmentStatus;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponseDto {
    private String id;
    private String cohortId;
    private String title;
    private String instructions;
    private SubmissionKind submissionType;
    private Integer points;
    private AssignmentStatus status;
}