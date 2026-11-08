package org.ssglobal.revalida.ssclassroom.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Composite primary key (cohort_id, user_id) for {@link CohortMemberModel}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CohortMemberId implements Serializable {

    private String cohortId;
    private String userId;
}
