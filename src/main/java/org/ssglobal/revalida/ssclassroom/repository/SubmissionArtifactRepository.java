package org.ssglobal.revalida.ssclassroom.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.SubmissionArtifactModel;

import java.util.Optional;


@Repository
public interface SubmissionArtifactRepository extends JpaRepository<SubmissionArtifactModel, String> {

  Optional<SubmissionArtifactModel> findByArtifactId(String artifactId);

  @Query("SELECT sa FROM SubmissionArtifactModel sa WHERE sa.submissionId = :submissionId")
  Optional<SubmissionArtifactModel> findBySubmissionId(@Param("submissionId") String submissionId);
}
