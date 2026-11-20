package org.ssglobal.revalida.ssclassroom.model;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CohortMemberId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String cohortId;
    private String userId;
}
