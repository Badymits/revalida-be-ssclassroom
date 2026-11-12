package org.ssglobal.revalida.ssclassroom.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.GradeFeedback.GradeFeedbackResponseDto;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.repository.GradeFeedbackRepository;
import org.ssglobal.revalida.ssclassroom.service.S3Bucket.S3Service;

// readOnly @Transactional follows AuthService/AssignmentService in this repo (not covered by the Java Training Notes).
@Service
@Transactional(readOnly = true)
public class GradeFeedbackService {

    private final GradeFeedbackRepository gradeFeedbackRepository;
    private final S3Service s3Service;
    private final String bucketName;

    public GradeFeedbackService(GradeFeedbackRepository gradeFeedbackRepository, S3Service s3Service,
            @Value("${aws.s3.bucket-name}") String bucketName) {
        this.gradeFeedbackRepository = gradeFeedbackRepository;
        this.s3Service = s3Service;
        this.bucketName = bucketName;
    }

    /**
     * @param userId the JWT subject. The query only matches this learner's own returned submission, so another
     *               learner's submission and a not-yet-returned one both come back as 404 (existence is not leaked).
     */
    public GradeFeedbackResponseDto getMyFeedback(String submissionId, String userId) {
        GradeFeedbackResponseDto row = gradeFeedbackRepository.findRow(submissionId, userId).stream()
                .findFirst()
                .orElseThrow(() -> NotFoundException.of("Graded submission", submissionId));

        // The query left the artifact's S3 key in artifactDownloadUrl; swap it for a short-lived signed URL.
        String s3Key = row.artifactDownloadUrl();
        return row.withDownloadUrl(s3Key == null
                ? null
                : s3Service.createPresignedGetUrl(bucketName, s3Key).getUploadUrl());
    }
}
