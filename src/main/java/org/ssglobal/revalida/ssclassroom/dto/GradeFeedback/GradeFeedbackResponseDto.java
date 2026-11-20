package org.ssglobal.revalida.ssclassroom.dto.GradeFeedback;

import java.time.Instant;

public record GradeFeedbackResponseDto(
        String submissionId,
        String assignmentId,
        String assignmentTitle,
        Integer assignmentPoints,
        Instant submittedAt,
        Integer gradePoints,
        String privateComment,
        Instant returnedAt,
        String trainerName,
        String artifactKind,
        String artifactName,
        String artifactBody,
        String artifactDownloadUrl) {

    public GradeFeedbackResponseDto withDownloadUrl(String url) {
        return new GradeFeedbackResponseDto(submissionId, assignmentId, assignmentTitle, assignmentPoints,
                submittedAt, gradePoints, privateComment, returnedAt, trainerName, artifactKind, artifactName, artifactBody, url);
    }
}
