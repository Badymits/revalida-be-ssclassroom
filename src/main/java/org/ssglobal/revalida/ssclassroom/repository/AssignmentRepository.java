package org.ssglobal.revalida.ssclassroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.enums.AssignmentStatus;
import org.ssglobal.revalida.ssclassroom.model.AssignmentModel;

@Repository
public interface AssignmentRepository extends JpaRepository<AssignmentModel, String> {

    List<AssignmentModel> findByCohort_CohortId(String cohortId);

    List<AssignmentModel> findByCohort_CohortIdAndStatus(String cohortId, AssignmentStatus status);

    // Learners only (trainers are skipped via app_user.role)
    @Query(value = """
            SELECT cm.user_id
            FROM cohort_member cm
            JOIN app_user u ON u.user_id = cm.user_id
            WHERE cm.cohort_id = :cohortId AND u.role = 'learner'
            """, nativeQuery = true)
    List<String> findLearnerIdsInCohort(@Param("cohortId") String cohortId);

    // Returns 1 if inserted, 0 if the learner already had a row
    @Modifying
    @Query(value = """
            INSERT INTO submission (submission_id, assignment_id, learner_id, status)
            VALUES (:submissionId, :assignmentId, :learnerId, 'assigned')
            ON CONFLICT (assignment_id, learner_id) DO NOTHING
            """, nativeQuery = true)
    int insertAssignedSubmission(@Param("submissionId") String submissionId,
                                 @Param("assignmentId") String assignmentId,
                                 @Param("learnerId") String learnerId);
}