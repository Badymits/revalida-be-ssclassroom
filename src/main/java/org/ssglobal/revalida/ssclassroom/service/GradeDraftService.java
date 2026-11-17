package org.ssglobal.revalida.ssclassroom.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.exceptions.BadRequestException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.exceptions.StateInvalidException;
import org.ssglobal.revalida.ssclassroom.model.GradeDraftModel;
import org.ssglobal.revalida.ssclassroom.repository.GradeDraftRepository;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.SubmissionForReturn;

@Service
@Transactional
public class GradeDraftService {

    private final GradeRepository gradeRepository;
    private final GradeDraftRepository gradeDraftRepository;

    @Autowired
    public GradeDraftService(GradeRepository gradeRepository,
            GradeDraftRepository gradeDraftRepository) {
        this.gradeRepository = gradeRepository;
        this.gradeDraftRepository = gradeDraftRepository;
    }

    public GradeDraftModel saveDraft(String submissionId, Integer points,
            String privateComment, String trainerId) {

        SubmissionForReturn submission = gradeRepository.findSubmissionForReturn(submissionId)
                .orElseThrow(() -> NotFoundException.of("Submission", submissionId));

        if (!"submitted".equals(submission.getStatus())) {
            throw new StateInvalidException("Only submitted work can have a draft");
        }

        if (points != null && points > submission.getPoints()) {
            throw new BadRequestException("points",
                    "Points cannot be more than " + submission.getPoints());
        }

        GradeDraftModel draft = gradeDraftRepository.findById(submissionId)
                .orElseGet(() -> new GradeDraftModel(submissionId));

        draft.setPoints(points);
        draft.setPrivateComment(privateComment == null ? "" : privateComment);
        draft.setUpdatedAt(Instant.now());
        draft.setUpdatedBy(trainerId);

        return gradeDraftRepository.save(draft);
    }

    public void deleteDraft(String submissionId) {
        gradeDraftRepository.deleteById(submissionId);
    }
}