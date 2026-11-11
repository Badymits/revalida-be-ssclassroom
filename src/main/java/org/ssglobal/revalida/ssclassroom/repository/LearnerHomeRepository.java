package org.ssglobal.revalida.ssclassroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.dto.LearnerHome.LearnerHomeResponseDto;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberId;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberModel;

/**
 * Read-only L1 query over Dev 1-3 tables. The SQL and its record mapping live on CohortMemberModel as
 * @NamedNativeQuery "LearnerHome.findRows" + @SqlResultSetMapping "LearnerHomeResponseDtoMapping".
 *
 * Reference: ORM_Standards [Part 1-pg72].docx (record DTO from native SQL). Calling a named query through a Spring Data
 * Repository with @Query(name=...) / @Param is NOT in the Java Training Notes; it follows Spring Data conventions.
 */
// Spring Data's marker base is fully qualified because its name clashes with the @Repository stereotype above.
@Repository
public interface LearnerHomeRepository extends JpaRepository<CohortMemberModel, CohortMemberId> {

    @Query(name = "LearnerHome.findRows", nativeQuery = true)
    List<LearnerHomeResponseDto> findRows(@Param("userId") String userId);
}
