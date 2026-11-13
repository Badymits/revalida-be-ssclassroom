package org.ssglobal.revalida.ssclassroom.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.ReturnResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.RosterResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.SubmissionDetailDto;
import org.ssglobal.revalida.ssclassroom.exceptions.ForbiddenException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.ArtifactRow;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.RosterRow;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.StatusCount;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.SubmissionRow;
import org.ssglobal.revalida.ssclassroom.service.S3Bucket.S3Service;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final GradeRepository gradeRepository;
    private final S3Service s3Service;
    private final NameEncryptionService nameEncryptionService;

    @Autowired
    public ReviewService(GradeRepository gradeRepository, S3Service s3Service,
            NameEncryptionService nameEncryptionService) {
        this.gradeRepository = gradeRepository;
        this.s3Service = s3Service;
        this.nameEncryptionService = nameEncryptionService;
    }

    public RosterResponseDto getRoster(String assignmentId, String status, int limit, String cursor) {
        if (!gradeRepository.assignmentExists(assignmentId)) {
            throw NotFoundException.of("Assignment", assignmentId);
        }

        List<RosterRow> rows = gradeRepository.findRosterPage(assignmentId, status, cursor, limit + 1);
        boolean hasNextPage = rows.size() > limit;
        List<RosterRow> page = hasNextPage ? rows.subList(0, limit) : rows;
        String nextCursor = hasNextPage ? page.get(limit - 1).getSubmissionId() : null;

        List<RosterResponseDto.Item> items = page.stream()
                .map(this::toRosterItem)
                .toList();

        return new RosterResponseDto(buildSummary(assignmentId), items, nextCursor);
    }

    public SubmissionDetailDto getSubmission(String submissionId, String userId, boolean isTrainer) {
        SubmissionRow submission = gradeRepository.findSubmissionDetail(submissionId)
                .orElseThrow(() -> NotFoundException.of("Submission", submissionId));

        if (!isTrainer && !submission.getLearnerId().equals(userId)) {
            throw new ForbiddenException("You can only view your own submission");
        }

        List<SubmissionDetailDto.Artifact> artifacts = gradeRepository.findArtifact(submissionId)
                .map(this::toArtifact)
                .map(List::of)
                .orElse(List.of());

        ReturnResponseDto.GradeDto grade = gradeRepository.findById(submissionId)
                .map(ReturnResponseDto.GradeDto::from)
                .orElse(null);

        return new SubmissionDetailDto(submission.getSubmissionId(), submission.getAssignmentId(),
                submission.getLearnerId(), submission.getStatus(), submission.getSubmittedAt(),
                artifacts, grade);
    }

    private RosterResponseDto.Item toRosterItem(RosterRow row) {
        String learnerLabel = nameEncryptionService.decrypt(row.getFirstName())
                + " " + nameEncryptionService.decrypt(row.getLastName());

        return new RosterResponseDto.Item(row.getSubmissionId(), row.getLearnerId(), learnerLabel,
                row.getStatus(), row.getPoints());
    }

    private RosterResponseDto.Summary buildSummary(String assignmentId) {
        Map<String, Long> counts = gradeRepository.countByStatus(assignmentId).stream()
                .collect(Collectors.toMap(StatusCount::getStatus, StatusCount::getTotal));

        long assigned = counts.getOrDefault("assigned", 0L);
        long submitted = counts.getOrDefault("submitted", 0L);
        long returned = counts.getOrDefault("returned", 0L);

        return new RosterResponseDto.Summary(assigned, submitted, returned,
                assigned + submitted + returned);
    }

    private SubmissionDetailDto.Artifact toArtifact(ArtifactRow artifact) {
        String downloadUrl = null;
        if (artifact.getS3Key() != null) {
            String bucket = s3Service.resolveBucketForFile(fileExtension(artifact.getName()));
            downloadUrl = s3Service.createPresignedGetUrl(bucket, artifact.getS3Key()).getUploadUrl();
        }

        return new SubmissionDetailDto.Artifact(artifact.getKind(), artifact.getName(),
                artifact.getBody(), downloadUrl);
    }

    private String fileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}