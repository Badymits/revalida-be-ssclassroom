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

    // ---------- Trainer side (includes drafts) ----------

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

        return toResponse(assignmentRepo.save(a));
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

        return toResponse(a);
    }

    @Transactional
    public PublishResponseDto publish(String assignmentId, PublishRequestDto req) {
        AssignmentModel a = find(assignmentId);

        if (a.getStatus() != AssignmentStatus.draft) {
            throw new StateInvalidException(
                    "Only draft assignments can be published (current: " + a.getStatus() + ")");
        }

        // Instructions are required before publishing
        if (a.getInstructions() == null || a.getInstructions().isBlank()) {
            throw new BadRequestException("Add instructions before publishing");
        }

        a.setStatus(AssignmentStatus.published);

        // Fan-out: one "assigned" submission per learner in the cohort (trainers skipped).
        // Same transaction as the status change, so it is all-or-nothing.
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

    // ---------- Learner side (published only, own cohort only) ----------

    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getMyPublishedAssignments(String userId) {
        CohortMemberModel member = memberRepo.findByUserId(userId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("You are not in a cohort"));

        return assignmentRepo
                .findByCohort_CohortIdAndStatus(member.getCohortId(), AssignmentStatus.published)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponseDto getByIdForLearner(String id, String userId) {
        AssignmentModel a = find(id);

        boolean isMember = memberRepo.existsByCohortIdAndUserId(
                a.getCohort().getCohortId(), userId);

        // same 404 for drafts and other cohorts, so hidden work isn't revealed
        if (a.getStatus() != AssignmentStatus.published || !isMember) {
            throw new NotFoundException("Assignment not found: " + id);
        }
        return toResponse(a);
    }

    // ---------- helpers ----------

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