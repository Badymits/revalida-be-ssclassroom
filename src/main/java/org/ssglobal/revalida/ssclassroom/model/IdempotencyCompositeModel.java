package org.ssglobal.revalida.ssclassroom.model;


import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class IdempotencyCompositeModel implements Serializable {

  @Column(name = "idem_key", unique = true, nullable = false)
  private String idemKey;

  @Column(name = "user_id", unique = true, nullable = false)
  private String userId;

  @Column(name = "submission_id", unique = true, nullable = false)
  private String submissionId;
}
