package org.ssglobal.revalida.ssclassroom.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AssignmentResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.AssignmentStatus;
import org.ssglobal.revalida.ssclassroom.model.AssignmentModel;

@Repository
public interface AssignmentRepository extends JpaRepository<AssignmentModel, String> {
    List<AssignmentModel> findByCohort_CohortId(String cohortId);

    List<AssignmentModel> findByCohort_CohortIdAndStatus(String cohortId, AssignmentStatus status);
  
}
