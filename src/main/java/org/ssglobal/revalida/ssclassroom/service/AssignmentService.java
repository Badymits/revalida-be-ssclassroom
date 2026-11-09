package org.ssglobal.revalida.ssclassroom.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// adjust these exception imports to your package
import org.ssglobal.revalida.ssclassroom.exceptions.BadRequestException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.exceptions.StateInvalidException;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AssignmentCreateRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AssignmentResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AssignmentUpdateRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Publish.PublishRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Publish.PublishResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.AssignmentStatus;
import org.ssglobal.revalida.ssclassroom.model.AssignmentModel;
import org.ssglobal.revalida.ssclassroom.model.CohortModel;
import org.ssglobal.revalida.ssclassroom.repository.AssignmentRepository;
import org.ssglobal.revalida.ssclassroom.repository.CohortRepository;


import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AssignmentCreateRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AssignmentResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Assignment.AssignmentUpdateRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Publish.PublishRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Publish.PublishResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.AssignmentStatus;
import org.ssglobal.revalida.ssclassroom.exceptions.BadRequestException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.exceptions.StateInvalidException;
import org.ssglobal.revalida.ssclassroom.model.AssignmentModel;
import org.ssglobal.revalida.ssclassroom.model.CohortModel;
import org.ssglobal.revalida.ssclassroom.repository.AssignmentRepository;
import org.ssglobal.revalida.ssclassroom.repository.CohortRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepo;
    private final CohortRepository cohortRepo;

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getByCohort(String cohortId) {
        return assignmentRepo.findByCohort_CohortId(cohortId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponseDto getById(String id) {
        return toResponse(find(id));
    }

    @Transactional
    public AssignmentResponseDto create(String cohortId, AssignmentCreateRequestDto req) {
        CohortModel cohort = cohortRepo.findById(cohortId)
                .orElseThrow(() -> new NotFoundException("Cohort not found: " + cohortId));

        AssignmentModel a = AssignmentModel.builder()
                .cohort(cohort)
                .title(req.getTitle())
                .instructions(req.getInstructions() != null ? req.getInstructions() : "")
                .submissionType(req.getSubmissionType())
                .points(req.getPoints() != null ? req.getPoints() : 100)
                .build();

        return toResponse(assignmentRepo.save(a));   // model rules are checked when this is flushed
    }

    @Transactional
    public AssignmentResponseDto update(String id, AssignmentUpdateRequestDto req) {
        if (req.getTitle() == null && req.getInstructions() == null
                && req.getSubmissionType() == null && req.getPoints() == null) {
            throw new BadRequestException("No fields to update");
        }

        AssignmentModel a = find(id);

        if (req.getTitle() != null)          a.setTitle(req.getTitle());
        if (req.getInstructions() != null)   a.setInstructions(req.getInstructions());
        if (req.getSubmissionType() != null) a.setSubmissionType(req.getSubmissionType());
        if (req.getPoints() != null)         a.setPoints(req.getPoints());

        return toResponse(a);   // no save() needed, and model rules are checked at commit
    }

    @Transactional
    public PublishResponseDto publish(String assignmentId, PublishRequestDto req) {
        AssignmentModel a = find(assignmentId);

        if (a.getStatus() != AssignmentStatus.draft) {
            throw new StateInvalidException(
                    "Only draft assignments can be published (current: " + a.getStatus() + ")");
        }

        a.setStatus(AssignmentStatus.published);  
        return null;
    }

    private AssignmentModel find(String id) {
        return assignmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found: " + id));
    }

    private AssignmentResponseDto toResponse(AssignmentModel a) {
        return AssignmentResponseDto.builder()
                .id(a.getAssignmentId())
                .cohortId(a.getCohort().getCohortId())
                .title(a.getTitle())
                .instructions(a.getInstructions())
                .submissionType(a.getSubmissionType())
                .points(a.getPoints())
                .status(a.getStatus())
                .build();
    }
}