package org.ssglobal.revalida.ssclassroom.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.ssglobal.revalida.ssclassroom.enums.Status;

import java.time.LocalDateTime;


@Data
@Entity
public class SubmissionModel {

    @Id
    private String submissionId;

    @Id
    private String assignmentId; // will reference assignment Id once assignment model is created

    @Id
    private String learnerId;

    @Column(name = "status")
    private Status status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OneToOne(
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(name = "artifactId") // owns the foreign key
    private SubmissionArtifactModel submissionArtifact;
    private String grade;



}
