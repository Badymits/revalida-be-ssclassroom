package org.ssglobal.revalida.ssclassroom.dto.LearnerHome;

import java.time.Instant;

/**
 * One row on the L1 Learner home: a published assignment plus the caller's own submission state.
 * Flat on purpose: the record's canonical constructor matches the 11 query columns, so @ConstructorResult fills it directly.
 *
 * Reference: Java Training Notes / ORM_Standards [Part 1-pg72].docx
 *  - "Java Record in Hibernate 7" + "Use of Java Record as DTO Projections" (read-only record DTO)
 *  - "Use of Java Record for Native SQL Queries (@SqlResultSetMapping)" (record filled via @ConstructorResult)
 * Naming (Request/Response suffix, dto/<Screen>/ folder) follows existing dto/Assignment and dto/Auth, not the notes.
 */
public record LearnerHomeResponseDto(
        String assignmentId,
        String cohortId,
        String cohortName,
        String title,
        String submissionType,
        /** assignment maximum points */
        Integer points,
        /** null when the learner joined after publish (no submission row was fanned out) */
        String submissionId,
        String status,
        Instant submittedAt,
        /** awarded points; null until the submission is returned (null together with gradeReturnedAt) */
        Integer gradePoints,
        Instant gradeReturnedAt) {
}
