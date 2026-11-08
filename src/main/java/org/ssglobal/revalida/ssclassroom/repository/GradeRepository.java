package org.ssglobal.revalida.ssclassroom.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.GradeModel;

@Repository
public interface GradeRepository extends JpaRepository<GradeModel, String> {

    interface SubmissionForReturn {
        String getStatus();
        Integer getPoints();
    }

    interface RosterRow {
        String getSubmissionId();
        String getLearnerId();
        String getFirstName();
        String getLastName();
        String getStatus();
        Integer getPoints();
    }

    interface StatusCount {
        String getStatus();
        Long getTotal();
    }

    interface SubmissionRow {
        String getSubmissionId();
        String getAssignmentId();
        String getLearnerId();
        String getStatus();
        Instant getSubmittedAt();
    }

    interface ArtifactRow {
        String getKind();
        String getName();
        String getBody();
        String getS3Key();
    }

    @Query(value = """
            SELECT s.status AS status, a.points AS points FROM submission s
            JOIN assignment a ON a.assignment_id = s.assignment_id
            WHERE s.submission_id = :id
            """, nativeQuery = true)
    Optional<SubmissionForReturn> findSubmissionForReturn(@Param("id") String submissionId);

    @Modifying
    @Query(value = """
            UPDATE submission SET status = 'returned'
            WHERE submission_id = :id AND status = 'submitted'
            """, nativeQuery = true)
    int markReturned(@Param("id") String submissionId);

    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM assignment WHERE assignment_id = :id)
            """, nativeQuery = true)
    boolean assignmentExists(@Param("id") String assignmentId);

    @Query(value = """
            SELECT s.submission_id AS "submissionId",
                   s.learner_id AS "learnerId",
                   u.first_name AS "firstName",
                   u.last_name AS "lastName",
                   s.status AS status,
                   g.points AS points
            FROM submission s
            JOIN app_user u ON u.user_id = s.learner_id
            LEFT JOIN grade g ON g.submission_id = s.submission_id
            WHERE s.assignment_id = :assignmentId
              AND (CAST(:status AS VARCHAR) IS NULL OR s.status = CAST(:status AS VARCHAR))
              AND (CAST(:cursor AS VARCHAR) IS NULL OR s.submission_id > CAST(:cursor AS VARCHAR))
            ORDER BY s.submission_id
            LIMIT :limit
            """, nativeQuery = true)
    List<RosterRow> findRosterPage(@Param("assignmentId") String assignmentId,
            @Param("status") String status,
            @Param("cursor") String cursor,
            @Param("limit") int limit);

    @Query(value = """
            SELECT status AS status, COUNT(*) AS total FROM submission
            WHERE assignment_id = :assignmentId
            GROUP BY status
            """, nativeQuery = true)
    List<StatusCount> countByStatus(@Param("assignmentId") String assignmentId);

    @Query(value = """
            SELECT submission_id AS "submissionId",
                   assignment_id AS "assignmentId",
                   learner_id AS "learnerId",
                   status AS status,
                   submitted_at AS "submittedAt"
            FROM submission
            WHERE submission_id = :id
            """, nativeQuery = true)
    Optional<SubmissionRow> findSubmissionDetail(@Param("id") String submissionId);

    @Query(value = """
            SELECT kind AS kind, name AS name, body AS body, s3_key AS "s3Key"
            FROM submission_artifact
            WHERE submission_id = :id
            LIMIT 1
            """, nativeQuery = true)
    Optional<ArtifactRow> findArtifact(@Param("id") String submissionId);
}