package org.ssglobal.revalida.ssclassroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.dto.GradeFeedback.GradeFeedbackResponseDto;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberId;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberModel;

/**
 * Read-only L3 query over Dev 1-3 tables. The SQL and its record mapping live on CohortMemberModel as
 * @NamedNativeQuery "GradeFeedback.findRow" + @SqlResultSetMapping "GradeFeedbackResponseDtoMapping".
 *
 * Same pattern as LearnerHomeRepository (see its note on references).
 */
@Repository
public interface GradeFeedbackRepository extends JpaRepository<CohortMemberModel, CohortMemberId> {

    /** Empty unless the submission exists, belongs to userId and is returned (graded). */
    @Query(name = "GradeFeedback.findRow", nativeQuery = true)
    List<GradeFeedbackResponseDto> findRow(@Param("submissionId") String submissionId, @Param("userId") String userId);
}
