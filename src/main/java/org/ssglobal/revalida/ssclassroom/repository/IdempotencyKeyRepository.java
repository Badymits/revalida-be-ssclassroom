package org.ssglobal.revalida.ssclassroom.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.IdempotencyCompositeModel;
import org.ssglobal.revalida.ssclassroom.model.IdempotencyKeyModel;

import java.util.Optional;


@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyModel, IdempotencyCompositeModel> {

  @Query("SELECT k FROM IdempotencyKeyModel k WHERE k.idempotencyCompositeModel.idemKey = :idemKey")
  Optional<IdempotencyKeyModel> findByIdemKey(@Param("idemKey") String idemKey);
}
