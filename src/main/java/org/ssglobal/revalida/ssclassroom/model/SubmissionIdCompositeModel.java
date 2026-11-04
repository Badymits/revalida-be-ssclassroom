package org.ssglobal.revalida.ssclassroom.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Id;
import org.ssglobal.revalida.ssclassroom.service.PrefixedIdService;

import java.io.Serializable;


@Embeddable
public class SubmissionIdCompositeModel implements Serializable {

  @PrefixedIdService(prefix = "sub_")
  @Column(name = "submission_id", nullable = false, unique = true)
  private String submissionId;

  @Column(name = "assignment_id", nullable = false, unique = true)
  private String assignmentId;

  @Column(name = "learner_id", nullable = false, unique = true)
  private String learnerId;
}
