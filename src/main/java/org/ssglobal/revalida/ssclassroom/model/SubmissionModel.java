package org.ssglobal.revalida.ssclassroom.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Data
public class SubmissionModel {

    private Integer submissionId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getSubmissionId() {
        return submissionId;
    }

}
