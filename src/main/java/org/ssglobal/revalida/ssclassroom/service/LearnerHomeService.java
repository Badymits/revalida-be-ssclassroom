package org.ssglobal.revalida.ssclassroom.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.LearnerHome.LearnerHomeResponseDto;
import org.ssglobal.revalida.ssclassroom.repository.AssignmentRepository;
import org.ssglobal.revalida.ssclassroom.repository.LearnerHomeRepository;

import com.github.f4b6a3.ulid.UlidCreator;

import lombok.RequiredArgsConstructor;

// @Transactional follows AuthService/AssignmentService in this repo (not covered by the Java Training Notes).
@Service
@RequiredArgsConstructor
public class LearnerHomeService {

    private final LearnerHomeRepository learnerHomeRepository;
    private final AssignmentRepository assignmentRepository;

    /**
     * @param userId the JWT subject; only that learner's own cohort and submissions are read.
     *
     * Publishing only creates an "assigned" submission for learners who are already in the cohort. A learner who joined
     * later would have no submission row, so every assignment would show as Assigned but could not be opened (the
     * workspace is keyed by submission id). So any missing row is created here, the same way publish does it.
     * insertAssignedSubmission is ON CONFLICT DO NOTHING, so repeated or concurrent loads never create duplicates.
     */
    @Transactional
    public List<LearnerHomeResponseDto> listMyAssignments(String userId) {
        for (String assignmentId : learnerHomeRepository.findPublishedAssignmentIdsWithoutSubmission(userId)) {
            assignmentRepository.insertAssignedSubmission("sub_" + UlidCreator.getMonotonicUlid(), assignmentId, userId);
        }
        return learnerHomeRepository.findRows(userId);
    }
}
