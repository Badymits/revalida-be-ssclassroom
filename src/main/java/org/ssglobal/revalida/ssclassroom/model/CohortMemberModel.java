package org.ssglobal.revalida.ssclassroom.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.ssglobal.revalida.ssclassroom.enums.Role;
import org.ssglobal.revalida.ssclassroom.service.PrefixedIdService;

@Entity
@Table(name = "cohort_member",
       indexes = @Index(name = "idx_cohort_member_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CohortMemberModel {

   @Id
   @Column(name = "cohort_id", length = 30, nullable = false, updatable = false)
   @ToString.Include
   @EqualsAndHashCode.Include
   private String cohortId;
   
   @Id
   @PrefixedIdService(prefix = "usr_")
   @Column(name = "user_id", length = 30)
   private String userId;
   
   
   @Enumerated(EnumType.STRING)
   @Column(name = "role", nullable = false, length = 10)
   private Role role;

    @NotBlank
    @Size(max = 120)
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
}