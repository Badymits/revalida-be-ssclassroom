package org.ssglobal.revalida.ssclassroom.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grade")
public class GradeModel {

    private String submissionId;
    private Integer points;
    private String privateComment;
    private Instant returnedAt;
    private String returnedBy;

    public GradeModel() {
    }

    public GradeModel(String submissionId, Integer points, String privateComment,
            Instant returnedAt, String returnedBy) {
        this.submissionId = submissionId;
        this.points = points;
        this.privateComment = privateComment;
        this.returnedAt = returnedAt;
        this.returnedBy = returnedBy;
    }

    @Id
    @Column(name = "submission_id", length = 30)
    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    @Column(name = "points", nullable = false)
    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    @Column(name = "private_comment", nullable = false, length = 5000)
    public String getPrivateComment() {
        return privateComment;
    }

    public void setPrivateComment(String privateComment) {
        this.privateComment = privateComment;
    }

    @Column(name = "returned_at", nullable = false)
    public Instant getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }

    @Column(name = "returned_by", nullable = false, length = 30)
    public String getReturnedBy() {
        return returnedBy;
    }

    public void setReturnedBy(String returnedBy) {
        this.returnedBy = returnedBy;
    }
}