package org.ssglobal.revalida.ssclassroom.repository;

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
}