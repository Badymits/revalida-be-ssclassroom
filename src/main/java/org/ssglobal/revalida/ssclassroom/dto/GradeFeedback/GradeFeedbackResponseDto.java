package org.ssglobal.revalida.ssclassroom.dto.GradeFeedback;

import java.time.Instant;

/**
 * Response of GET /api/v1/me/submissions/{submissionId}/feedback: one returned (graded) submission of the caller,
 * flat, with the assignment title and maximum points the L3 screen needs.
 *
 * Reference: Java Training Notes / ORM_Standards [Part 1-pg72].docx (Java record as a response DTO).
 */
public record GradeFeedbackResponseDto(
        String submissionId,
        String assignmentId,
        String assignmentTitle,
        Integer assignmentPoints,
        Instant submittedAt,
        Integer gradePoints,
        String privateComment,
        Instant returnedAt,
        String artifactKind,        // null when the submission has no artifact
        String artifactName,
        String artifactBody,
        String artifactDownloadUrl) {  // pre-signed and time-boxed; null for text artifacts

    /**
     * The native query puts the artifact's S3 key in artifactDownloadUrl (records have no setters). The service
     * replaces it with a signed URL before returning, so the raw key never leaves the backend.
     */
    public GradeFeedbackResponseDto withDownloadUrl(String url) {
        return new GradeFeedbackResponseDto(submissionId, assignmentId, assignmentTitle, assignmentPoints,
                submittedAt, gradePoints, privateComment, returnedAt, artifactKind, artifactName, artifactBody, url);
    }
}
