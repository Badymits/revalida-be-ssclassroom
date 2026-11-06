package org.ssglobal.revalida.ssclassroom.model;

import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionType;
import org.ssglobal.revalida.ssclassroom.service.PrefixedIdService;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.ssglobal.revalida.ssclassroom.enums.AssignmentStatus;
import org.ssglobal.revalida.ssclassroom.enums.Role;
import org.ssglobal.revalida.ssclassroom.service.PrefixedIdService;

import java.util.List;

@Entity
@Table(name = "assignment",
       indexes = @Index(name = "idx_assignment_cohort", columnList = "cohort_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentModel {

    @Id
    @PrefixedIdService(prefix = "asg_")
    @Column(name = "assignment_id", length = 30)
    private String assignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_id", nullable = false)
    private CohortModel cohort;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Size(max = 50000)
    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String instructions = "";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, length = 10)
    private SubmissionType submissionType;

    @Min(0) @Max(1000)
    @Column(nullable = false)
    @Builder.Default
    private int points = 100;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.Draft;

    @OneToMany(
            fetch = FetchType.LAZY,
            targetEntity = SubmissionModel.class,
            mappedBy = "assignmentModel"
    )
    private List<SubmissionModel> submissionModelList;
}