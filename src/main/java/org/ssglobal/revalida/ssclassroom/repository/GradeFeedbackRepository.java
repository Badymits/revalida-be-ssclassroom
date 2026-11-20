package org.ssglobal.revalida.ssclassroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.dto.GradeFeedback.GradeFeedbackResponseDto;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberId;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberModel;

@Repository
public interface GradeFeedbackRepository extends JpaRepository<CohortMemberModel, CohortMemberId> {

    @Query(value = """
            SELECT
                -- submission
                s.submission_id                      AS submission_id,

                -- assignment
                a.assignment_id                      AS assignment_id,
                a.title                              AS assignment_title,
                a.points                             AS assignment_points,

                -- submission
                s.submitted_at                       AS submitted_at,

                -- grade
                g.points                             AS grade_points,
                g.private_comment                    AS private_comment,
                g.returned_at                        AS returned_at,

                NULLIF(TRIM(CONCAT_WS(' ', t.first_name, t.last_name)), '') AS trainer_name,

                sa.kind                              AS artifact_kind,
                sa.name                              AS artifact_name,
                sa.body                              AS artifact_body,
                sa.s3_key                            AS artifact_s3_key
            FROM submission s
            JOIN assignment a
                ON a.assignment_id = s.assignment_id
            JOIN grade g
                ON g.submission_id = s.submission_id
            LEFT JOIN app_user t
                ON t.user_id = g.returned_by
            LEFT JOIN LATERAL (
                SELECT kind, name, body, s3_key
                FROM submission_artifact
                WHERE submission_id = s.submission_id
                LIMIT 1
            ) sa ON TRUE
            WHERE s.submission_id = :submissionId
                AND s.learner_id = :userId
                AND s.status = 'returned'
            """, nativeQuery = true)
    List<GradeFeedbackResponseDto> findRow(@Param("submissionId") String submissionId, @Param("userId") String userId);
}
