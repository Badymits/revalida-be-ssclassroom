package org.ssglobal.revalida.ssclassroom.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.ssglobal.revalida.ssclassroom.enums.Status;

import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Table(name = "submission")
public class SubmissionModel {

    @EmbeddedId
    private SubmissionIdCompositeModel submissionCompositeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    // Setting this is mostly for Java object-graph navigation (fetching titles, etc.)
    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("assignmentId")
    @JoinColumn(
            name = "assignment_id",
            nullable = false
    )
    private AssignmentModel assignmentModel;

}
