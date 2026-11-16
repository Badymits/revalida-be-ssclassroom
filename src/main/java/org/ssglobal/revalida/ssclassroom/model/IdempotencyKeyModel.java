package org.ssglobal.revalida.ssclassroom.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "idempotency_key")
public class IdempotencyKeyModel {

  @EmbeddedId
  private IdempotencyCompositeModel idempotencyCompositeModel;

  @Column(name = "created_at")
  private Instant createdAt;
}
