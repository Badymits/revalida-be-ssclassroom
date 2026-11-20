package org.ssglobal.revalida.ssclassroom.model;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

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
