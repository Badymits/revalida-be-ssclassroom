package org.ssglobal.revalida.ssclassroom.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grade_draft")
public class GradeDraftModel {

    private String submissionId;
    private Integer points;
    private String privateComment;
    private Instant updatedAt;
    private String updatedBy;

    public GradeDraftModel() {
    }

    public GradeDraftModel(String submissionId) {
        this.submissionId = submissionId;
    }

    @Id
    @Column(name = "submission_id", length = 30)
    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    @Column(name = "points")
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

    @Column(name = "updated_at", nullable = false)
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Column(name = "updated_by", nullable = false, length = 30)
    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}