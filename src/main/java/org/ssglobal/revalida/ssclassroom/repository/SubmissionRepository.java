package org.ssglobal.revalida.ssclassroom.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<SubmissionModel, String> {

  Optional<List<SubmissionModel>> findByAssignmentId(String assignmentId);
}
