package org.ssglobal.revalida.ssclassroom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.GradeDraftModel;

@Repository
public interface GradeDraftRepository extends JpaRepository<GradeDraftModel, String> {
}