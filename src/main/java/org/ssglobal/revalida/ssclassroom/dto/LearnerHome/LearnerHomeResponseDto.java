package org.ssglobal.revalida.ssclassroom.dto.LearnerHome;

import java.time.Instant;

public record LearnerHomeResponseDto(
        String assignmentId,
        String cohortId,
        String cohortName,
        String title,
        String submissionType,
        Integer points,
        String submissionId,
        String status,
        Instant submittedAt,
        Integer gradePoints,
        Instant gradeReturnedAt) {
}
