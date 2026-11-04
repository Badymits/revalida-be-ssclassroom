package org.ssglobal.revalida.ssclassroom.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.exceptions.BadRequestException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.exceptions.StateInvalidException;
import org.ssglobal.revalida.ssclassroom.model.GradeModel;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.SubmissionForReturn;

@Service
@Transactional
public class GradeService {

    private static final String NOT_SUBMITTED = "Only submitted work can be returned";

    private final GradeRepository gradeRepository;

    @Autowired
    public GradeService(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

    public GradeModel returnSubmission(String submissionId, Integer points,
            String privateComment, String trainerId) {

        SubmissionForReturn submission = gradeRepository.findSubmissionForReturn(submissionId)
                .orElseThrow(() -> NotFoundException.of("Submission", submissionId));

        if (!"submitted".equals(submission.getStatus())) {
            throw new StateInvalidException(NOT_SUBMITTED);
        }

        if (points > submission.getPoints()) {
            throw new BadRequestException("points",
                    "Points cannot be more than " + submission.getPoints());
        }

        if (gradeRepository.markReturned(submissionId) == 0) {
            throw new StateInvalidException(NOT_SUBMITTED);
        }

        String comment = privateComment == null ? "" : privateComment;
        GradeModel grade = new GradeModel(submissionId, points, comment, Instant.now(), trainerId);
        gradeRepository.save(grade);

        return grade;
    }

    @Transactional(readOnly = true)
    public Optional<GradeModel> getGrade(String submissionId) {
        return gradeRepository.findById(submissionId);
    }
}