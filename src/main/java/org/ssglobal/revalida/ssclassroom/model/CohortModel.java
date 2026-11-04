package org.ssglobal.revalida.ssclassroom.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.ssglobal.revalida.ssclassroom.service.PrefixedIdService;

@Entity
@Table(name = "cohort")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CohortModel {

    @Id
    @PrefixedIdService(prefix = "crs_")
    @Column(name = "cohort_id", length = 30)
    private String cohortId;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;
}