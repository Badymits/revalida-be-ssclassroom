package org.ssglobal.revalida.ssclassroom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssglobal.revalida.ssclassroom.model.AssignmentAttachmentModel;

@Repository
public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachmentModel, String> {

    List<AssignmentAttachmentModel> findByAssignmentIdOrderBySortOrderAsc(String assignmentId);

    @Modifying(flushAutomatically = true)
    @Query("delete from AssignmentAttachmentModel a where a.assignmentId = :assignmentId")
    void deleteAllByAssignment(@Param("assignmentId") String assignmentId);
}
