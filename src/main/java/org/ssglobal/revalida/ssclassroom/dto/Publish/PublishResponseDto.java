package org.ssglobal.revalida.ssclassroom.dto.Publish;

import org.ssglobal.revalida.ssclassroom.enums.AssignmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishResponseDto {
    private String assignmentId;
    private AssignmentStatus status;
    private int submissionsCreated;
}
