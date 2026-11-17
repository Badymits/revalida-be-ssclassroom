package org.ssglobal.revalida.ssclassroom.model;

import org.ssglobal.revalida.ssclassroom.enums.AttachmentKind;
import org.ssglobal.revalida.ssclassroom.service.PrefixedIdService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "assignment_attachment")
public class AssignmentAttachmentModel {

  @Id
  @PrefixedIdService(prefix = "att_")
  @Column(name = "attachment_id", length = 30)
  private String attachmentId;

  @Column(name = "assignment_id", nullable = false, length = 30)
  private String assignmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 10)
  private AttachmentKind kind;

  @Column(name = "name")
  private String name;

  @Column(name = "content_type")
  private String contentType;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(name = "s3_key")
  private String s3Key;

  @Column(name = "url")
  private String url;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;
}
