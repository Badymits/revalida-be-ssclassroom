package org.ssglobal.revalida.ssclassroom.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.AppUserModel;

@Repository
public interface AppUserRepository extends JpaRepository<AppUserModel, String> {

    Optional<AppUserModel> findByUsername(String username);

    boolean existsByUsername(String username);
}
