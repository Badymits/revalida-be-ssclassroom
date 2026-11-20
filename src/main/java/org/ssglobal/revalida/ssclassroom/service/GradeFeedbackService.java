package org.ssglobal.revalida.ssclassroom.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.GradeFeedback.GradeFeedbackResponseDto;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.repository.GradeFeedbackRepository;
import org.ssglobal.revalida.ssclassroom.service.S3Bucket.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeFeedbackService {

    private final GradeFeedbackRepository gradeFeedbackRepository;
    private final S3Service s3Service;

    public GradeFeedbackResponseDto getMyFeedback(String submissionId, String userId) {
        GradeFeedbackResponseDto row = gradeFeedbackRepository.findRow(submissionId, userId).stream()
                .findFirst()
                .orElseThrow(() -> NotFoundException.of("Graded submission", submissionId));

        return row.withDownloadUrl(signedDownloadUrl(row.artifactName(), row.artifactDownloadUrl()));
    }

    private String signedDownloadUrl(String fileName, String s3Key) {
        if (s3Key == null || fileName == null || fileName.lastIndexOf('.') < 0) {
            return null;
        }
        try {
            String bucket = s3Service.resolveBucketForFile(fileName.substring(fileName.lastIndexOf('.') + 1));
            return s3Service.createPresignedGetUrl(bucket, s3Key).getUploadUrl();
        } catch (RuntimeException e) {
            log.warn("No download link for {}: {}", s3Key, e.getMessage());
            return null;
        }
    }
}