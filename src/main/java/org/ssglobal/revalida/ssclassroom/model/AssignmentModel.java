package org.ssglobal.revalida.ssclassroom.model;

import java.time.Instant;
import java.util.List;

import org.ssglobal.revalida.ssclassroom.enums.AssignmentStatus;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionType;
import org.ssglobal.revalida.ssclassroom.service.PrefixedIdService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private AssignmentStatus status = AssignmentStatus.draft;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Set when the assignment is published; null for drafts. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(
            fetch = FetchType.LAZY,
            targetEntity = SubmissionModel.class,
            mappedBy = "assignmentModel"
    )
    private List<SubmissionModel> submissionModelList;
}