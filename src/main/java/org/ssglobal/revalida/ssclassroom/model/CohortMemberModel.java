package org.ssglobal.revalida.ssclassroom.model;

import java.time.Instant;

import org.ssglobal.revalida.ssclassroom.dto.GradeFeedback.GradeFeedbackResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.LearnerHome.LearnerHomeResponseDto;

import jakarta.persistence.*;
import lombok.*;

/**
 * Links a LEARNER to their single cohort. Trainers never have a row here (enforced in the service layer);
 * a learner has at most one row (unique index on user_id, V6).
 * Role and display name live on app_user.
 *
 * Also holds the L1 Learner home read query (used by LearnerHomeRepository). Drafts are excluded; a published
 * assignment with no submission row (learner joined after publish) is 'assigned'; the grade is joined only for
 * returned submissions. Columns map by label into LearnerHomeResponseDto's canonical constructor.
 *
 * Reference: Java Training Notes / ORM_Standards [Part 1-pg72].docx, "Use of Java Record for Native SQL Queries
 * (@SqlResultSetMapping)" + "Use of Java Record Native SQL Mapping Implementation" (@NamedNativeQuery +
 * @SqlResultSetMapping + @ConstructorResult into a record).
 * SQL: SQL_Fundamentals_PostgreSQL_Standards.docx ("Coalescing: The COALESCE Function") and
 * SQL_Fundamentals_PostgreSQL_Standards [Part 2].docx ("Left Join").
 * Also holds the L3 Grade and feedback read query (GradeFeedbackRepository): one returned submission of one learner,
 * with its assignment, grade and first artifact. Same record-mapping pattern as above.
 * Placing these on CohortMemberModel is a project choice; the notes do not specify which entity holds it.
 */
@SqlResultSetMapping(
        name = "LearnerHomeResponseDtoMapping",
        classes = @ConstructorResult(
                targetClass = LearnerHomeResponseDto.class,
                columns = {
                        @ColumnResult(name = "assignment_id"),
                        @ColumnResult(name = "cohort_id"),
                        @ColumnResult(name = "cohort_name"),
                        @ColumnResult(name = "title"),
                        @ColumnResult(name = "submission_type"),
                        @ColumnResult(name = "points", type = Integer.class),
                        @ColumnResult(name = "submission_id"),
                        @ColumnResult(name = "status"),
                        @ColumnResult(name = "submitted_at", type = Instant.class),
                        @ColumnResult(name = "grade_points", type = Integer.class),
                        @ColumnResult(name = "grade_returned_at", type = Instant.class)
                }))
@NamedNativeQuery(
        name = "LearnerHome.findRows",
        resultSetMapping = "LearnerHomeResponseDtoMapping",
        query = """
                SELECT a.assignment_id, c.cohort_id, c.name AS cohort_name, a.title, a.submission_type, a.points,
                       s.submission_id, COALESCE(s.status, 'assigned') AS status, s.submitted_at,
                       g.points AS grade_points, g.returned_at AS grade_returned_at
                FROM cohort_member cm
                JOIN cohort c ON c.cohort_id = cm.cohort_id
                JOIN assignment a ON a.cohort_id = c.cohort_id AND a.status = 'published'
                LEFT JOIN submission s ON s.assignment_id = a.assignment_id AND s.learner_id = cm.user_id
                LEFT JOIN grade g ON g.submission_id = s.submission_id AND s.status = 'returned'
                WHERE cm.user_id = :userId
                ORDER BY a.assignment_id
                """)
@SqlResultSetMapping(
        name = "GradeFeedbackResponseDtoMapping",
        classes = @ConstructorResult(
                targetClass = GradeFeedbackResponseDto.class,
                columns = {
                        @ColumnResult(name = "submission_id"),
                        @ColumnResult(name = "assignment_id"),
                        @ColumnResult(name = "assignment_title"),
                        @ColumnResult(name = "assignment_points", type = Integer.class),
                        @ColumnResult(name = "submitted_at", type = Instant.class),
                        @ColumnResult(name = "grade_points", type = Integer.class),
                        @ColumnResult(name = "private_comment"),
                        @ColumnResult(name = "returned_at", type = Instant.class),
                        @ColumnResult(name = "artifact_kind"),
                        @ColumnResult(name = "artifact_name"),
                        @ColumnResult(name = "artifact_body"),
                        @ColumnResult(name = "artifact_s3_key")   // lands in artifactDownloadUrl; the service swaps it for a signed URL
                }))
@NamedNativeQuery(
        name = "GradeFeedback.findRow",
        resultSetMapping = "GradeFeedbackResponseDtoMapping",
        query = """
                SELECT s.submission_id, a.assignment_id, a.title AS assignment_title, a.points AS assignment_points,
                       s.submitted_at, g.points AS grade_points, g.private_comment, g.returned_at,
                       sa.kind AS artifact_kind, sa.name AS artifact_name, sa.body AS artifact_body,
                       sa.s3_key AS artifact_s3_key
                FROM submission s
                JOIN assignment a ON a.assignment_id = s.assignment_id
                JOIN grade g ON g.submission_id = s.submission_id
                LEFT JOIN LATERAL (SELECT kind, name, body, s3_key
                                   FROM submission_artifact
                                   WHERE submission_id = s.submission_id
                                   LIMIT 1) sa ON TRUE
                WHERE s.submission_id = :submissionId AND s.learner_id = :userId AND s.status = 'returned'
                """)
@Entity
@Table(name = "cohort_member",
       indexes = @Index(name = "idx_cohort_member_user", columnList = "user_id"))
@IdClass(CohortMemberId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CohortMemberModel {

    @Id
    @Column(name = "cohort_id", length = 30, nullable = false, updatable = false)
    private String cohortId;

    @Id
    @Column(name = "user_id", length = 30, nullable = false, updatable = false)
    private String userId;
}
