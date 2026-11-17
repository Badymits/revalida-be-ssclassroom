package org.ssglobal.revalida.ssclassroom.dto.Assignment;

import java.time.Instant;
import java.util.List;

import org.ssglobal.revalida.ssclassroom.enums.AssignmentStatus;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionType;

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
    private SubmissionType submissionType;
    private Integer points;
    private AssignmentStatus status;
    private Instant createdAt;
    private Instant publishedAt;
    private List<AttachmentResponseDto> attachments;
}