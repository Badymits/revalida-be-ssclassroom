package org.ssglobal.revalida.ssclassroom.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.LearnerHome.LearnerHomeResponseDto;
import org.ssglobal.revalida.ssclassroom.repository.AssignmentRepository;
import org.ssglobal.revalida.ssclassroom.repository.LearnerHomeRepository;

import com.github.f4b6a3.ulid.UlidCreator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LearnerHomeService {

    private final LearnerHomeRepository learnerHomeRepository;
    private final AssignmentRepository assignmentRepository;

    @Transactional
    public List<LearnerHomeResponseDto> listMyAssignments(String userId) {
        for (String assignmentId : learnerHomeRepository.findPublishedAssignmentIdsWithoutSubmission(userId)) {
            assignmentRepository.insertAssignedSubmission("sub_" + UlidCreator.getMonotonicUlid(), assignmentId, userId);
        }
        return learnerHomeRepository.findRows(userId);
    }
}
