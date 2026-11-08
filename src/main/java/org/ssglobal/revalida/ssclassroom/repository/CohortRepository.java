package org.ssglobal.revalida.ssclassroom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.CohortModel;

@Repository
public interface CohortRepository extends JpaRepository<CohortModel, String> {
}