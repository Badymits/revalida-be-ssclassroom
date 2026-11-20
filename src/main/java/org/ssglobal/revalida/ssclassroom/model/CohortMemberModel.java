package org.ssglobal.revalida.ssclassroom.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cohort_member",
       indexes = @Index(name = "idx_cohort_member_user", columnList = "user_id"))
@IdClass(CohortMemberId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CohortMemberModel {

    @Id
    @Column(name = "cohort_id", length = 30, nullable = false, updatable = false)
    private String cohortId;

    @Id
    @Column(name = "user_id", length = 30, nullable = false, updatable = false)
    private String userId;
}
