package org.ssglobal.revalida.ssclassroom.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;


@Entity
@Getter
@Setter
@Table(name = "submission_artifact")
public class SubmissionArtifactModel {

  @Id
  private String artifactId;

  @Column(name = "submission_id", nullable = false)
  private String submissionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false)
  private SubmissionKind kind;

  @Column(name = "name", nullable = false)
  private String artifactName;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "s3_key")
  private String s3Key;

  private String body;

  @Column(unique = true)
  private String url;

//  @OneToOne(mappedBy = "submissionArtifact")
//  private SubmissionModel submissionModel;


}
