package org.ssglobal.revalida.ssclassroom.service;

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
import org.ssglobal.revalida.ssclassroom.model.CohortMemberModel;
import org.ssglobal.revalida.ssclassroom.model.CohortModel;
import org.ssglobal.revalida.ssclassroom.repository.AssignmentRepository;
import org.ssglobal.revalida.ssclassroom.repository.CohortMemberRepository;
import org.ssglobal.revalida.ssclassroom.repository.CohortRepository;

import com.github.f4b6a3.ulid.UlidCreator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepo;
    private final CohortRepository cohortRepo;
    private final CohortMemberRepository memberRepo;
    private final AssignmentAttachmentService attachmentService;

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getByCohort(String cohortId) {
        return assignmentRepo.findByCohort_CohortId(cohortId)
                .stream()
                .map(a -> toResponse(a, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponseDto getById(String id) {
        return toResponse(find(id), true);
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

        AssignmentModel saved = assignmentRepo.save(a);

        if (req.getAttachments() != null) {
            attachmentService.replaceAll(saved.getAssignmentId(), req.getAttachments());
        }

        return toResponse(saved, true);
    }

    @Transactional
    public AssignmentResponseDto update(String id, AssignmentUpdateRequestDto req) {
        if (req.getTitle() == null && req.getInstructions() == null
                && req.getSubmissionType() == null && req.getPoints() == null
                && req.getAttachments() == null) {
            throw new BadRequestException("No fields to update");
        }

        AssignmentModel a = find(id);

        if (req.getTitle() != null)          a.setTitle(req.getTitle());
        if (req.getInstructions() != null)   a.setInstructions(req.getInstructions());
        if (req.getSubmissionType() != null) a.setSubmissionType(req.getSubmissionType());
        if (req.getPoints() != null)         a.setPoints(req.getPoints());

        if (req.getAttachments() != null) {
            attachmentService.replaceAll(a.getAssignmentId(), req.getAttachments());
        }

        return toResponse(a, true);
    }

    @Transactional
    public PublishResponseDto publish(String assignmentId, PublishRequestDto req) {
        AssignmentModel a = find(assignmentId);

        if (a.getStatus() != AssignmentStatus.draft) {
            throw new StateInvalidException(
                    "Only draft assignments can be published (current: " + a.getStatus() + ")");
        }

        if (a.getInstructions() == null || a.getInstructions().isBlank()) {
            throw new BadRequestException("Add instructions before publishing");
        }

        a.setStatus(AssignmentStatus.published);

       
        int created = 0;
        for (String learnerId : assignmentRepo.findLearnerIdsInCohort(a.getCohort().getCohortId())) {
            String submissionId = "sub_" + UlidCreator.getMonotonicUlid();
            created += assignmentRepo.insertAssignedSubmission(
                    submissionId, a.getAssignmentId(), learnerId);
        }

        return PublishResponseDto.builder()
                .assignmentId(a.getAssignmentId())
                .status(a.getStatus())
                .submissionsCreated(created)
                .build();
    }


    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getMyPublishedAssignments(String userId) {
        CohortMemberModel member = memberRepo.findByUserId(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("You are not in a cohort"));

        return assignmentRepo
                .findByCohort_CohortIdAndStatus(member.getCohortId(), AssignmentStatus.published)
                .stream()
                .map(a -> toResponse(a, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponseDto getByIdForLearner(String id, String userId) {
        AssignmentModel a = find(id);

        boolean isMember = memberRepo.existsByCohortIdAndUserId(
                a.getCohort().getCohortId(), userId);

        if (a.getStatus() != AssignmentStatus.published || !isMember) {
            throw new NotFoundException("Assignment not found: " + id);
        }
        return toResponse(a, true);
    }


    private AssignmentModel find(String id) {
        return assignmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found: " + id));
    }

    private AssignmentResponseDto toResponse(AssignmentModel a, boolean withAttachments) {
        return AssignmentResponseDto.builder()
                .id(a.getAssignmentId())
                .cohortId(a.getCohort().getCohortId())
                .title(a.getTitle())
                .instructions(a.getInstructions())
                .submissionType(a.getSubmissionType())
                .points(a.getPoints())
                .status(a.getStatus())
                .attachments(withAttachments ? attachmentService.listFor(a.getAssignmentId()) : null)
                .build();
    }
}