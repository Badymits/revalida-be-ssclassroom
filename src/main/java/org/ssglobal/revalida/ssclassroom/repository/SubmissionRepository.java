package org.ssglobal.revalida.ssclassroom.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.SubmissionIdCompositeModel;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<SubmissionModel, SubmissionIdCompositeModel> {

  Optional<List<SubmissionModel>> findByAssignmentModel_AssignmentId(String assignmentId);

  @Query("SELECT s FROM SubmissionModel s WHERE s.submissionCompositeId.submissionId = :submissionId")
  Optional<SubmissionModel> findBySubmissionId(@Param("submissionId") String submissionId);
}
