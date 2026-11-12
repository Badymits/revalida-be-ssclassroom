package org.ssglobal.revalida.ssclassroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberId;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberModel;

@Repository
public interface CohortMemberRepository extends JpaRepository<CohortMemberModel, CohortMemberId> {

    List<CohortMemberModel> findByUserId(String userId);

    List<CohortMemberModel> findByCohortId(String cohortId);

    boolean existsByUserId(String userId);

    boolean existsByCohortIdAndUserId(String cohortId, String userId);   // add this
}