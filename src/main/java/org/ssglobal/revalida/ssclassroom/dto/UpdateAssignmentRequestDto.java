package org.ssglobal.revalida.ssclassroom.dto;

import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateAssignmentRequestDto {
    private String title;
    private String instructions;
    private SubmissionKind submissionType;
    private Integer points;
}