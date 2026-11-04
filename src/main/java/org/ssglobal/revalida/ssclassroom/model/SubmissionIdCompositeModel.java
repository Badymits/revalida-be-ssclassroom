package org.ssglobal.revalida.ssclassroom.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Id;

import java.io.Serializable;


@Embeddable
public class SubmissionIdCompositeModel implements Serializable {

  @Column(name = "submission_id", nullable = false, unique = true)
  private String submissionId;

  @Column(name = "assignment_id", nullable = false, unique = true)
  private String assignmentId; // will reference assignment Id once assignment model is created

  @Column(name = "learner_id", nullable = false, unique = true)
  private String learnerId;
}
