package org.ssglobal.revalida.ssclassroom.dto.Assignment;

import org.ssglobal.revalida.ssclassroom.enums.SubmissionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentUpdateRequestDto {
    private String title;
    private String instructions;
    private SubmissionType submissionType;
    private Integer points;
}