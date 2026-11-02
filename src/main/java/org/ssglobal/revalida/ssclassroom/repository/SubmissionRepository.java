package org.ssglobal.revalida.ssclassroom.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;

@Repository
public interface SubmissionRepository extends JpaRepository<SubmissionModel, Integer> {
}
