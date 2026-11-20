package org.ssglobal.revalida.ssclassroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.dto.LearnerHome.LearnerHomeResponseDto;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberId;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberModel;

/**
 * Read-only L1 query over Dev 1-3 tables. The SQL lives here, on the repository method.
 *
 * Drafts are excluded; a published assignment with no submission row (learner joined after publish) is 'assigned';
 * the grade is joined only for returned submissions. The columns are in the same order as
 * LearnerHomeResponseDto's canonical constructor, so Spring Data fills the record by position.
 *
 * Reference: SQL_Fundamentals_PostgreSQL_Standards.docx ("Coalescing: The COALESCE Function") and
 * SQL_Fundamentals_PostgreSQL_Standards [Part 2].docx ("Left Join").
 */
@Repository
public interface LearnerHomeRepository extends JpaRepository<CohortMemberModel, CohortMemberId> {

    @Query(value = """
            SELECT
                -- assignment
                a.assignment_id                      AS assignment_id,

                -- cohort
                c.cohort_id                          AS cohort_id,
                c.name                               AS cohort_name,

                -- assignment
                a.title                              AS title,
                a.submission_type                    AS submission_type,
                a.points                             AS points,

                -- submission ('assigned' when the learner has no row yet)
                s.submission_id                      AS submission_id,
                COALESCE(s.status, 'assigned')       AS status,
                s.submitted_at                       AS submitted_at,

                -- grade (only for returned submissions)
                g.points                             AS grade_points,
                g.returned_at                        AS grade_returned_at
            FROM cohort_member cm
            JOIN cohort c
                ON c.cohort_id = cm.cohort_id
            JOIN assignment a
                ON a.cohort_id = c.cohort_id
                AND a.status = 'published'
            LEFT JOIN submission s
                ON s.assignment_id = a.assignment_id
                AND s.learner_id = cm.user_id
            LEFT JOIN grade g
                ON g.submission_id = s.submission_id
                AND s.status = 'returned'
            WHERE cm.user_id = :userId
            ORDER BY a.assignment_id
            """, nativeQuery = true)
    List<LearnerHomeResponseDto> findRows(@Param("userId") String userId);

    /** Published assignments in the learner's cohort(s) that have no submission row for them yet (late joiners). */
    @Query(value = """
            SELECT a.assignment_id
            FROM cohort_member cm
            JOIN assignment a ON a.cohort_id = cm.cohort_id AND a.status = 'published'
            WHERE cm.user_id = :userId
              AND NOT EXISTS (
                  SELECT 1 FROM submission s
                  WHERE s.assignment_id = a.assignment_id AND s.learner_id = cm.user_id)
            """, nativeQuery = true)
    List<String> findPublishedAssignmentIdsWithoutSubmission(@Param("userId") String userId);
}
