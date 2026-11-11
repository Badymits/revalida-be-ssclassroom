package org.ssglobal.revalida.ssclassroom.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.LearnerHome.LearnerHomeResponseDto;
import org.ssglobal.revalida.ssclassroom.repository.LearnerHomeRepository;

import lombok.RequiredArgsConstructor;

// readOnly @Transactional follows AuthService/AssignmentService in this repo (not covered by the Java Training Notes).
@Service
@RequiredArgsConstructor
public class LearnerHomeService {

    private final LearnerHomeRepository learnerHomeRepository;

    /** @param userId the JWT subject; only that learner's own cohort and submissions are read */
    @Transactional(readOnly = true)
    public List<LearnerHomeResponseDto> listMyAssignments(String userId) {
        return learnerHomeRepository.findRows(userId);
    }
}
