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
    private SubmissionIdCompositeModel submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

//    @OneToOne(
//            cascade = CascadeType.ALL,
//            orphanRemoval = true
//    )
//    @JoinColumn(name = "artifactId") // owns the foreign key
//    private SubmissionArtifactModel submissionArtifact;

}
