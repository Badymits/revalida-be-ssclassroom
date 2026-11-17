package org.ssglobal.revalida.ssclassroom.review.service.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;
import org.ssglobal.revalida.ssclassroom.dto.RosterResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.SubmissionDetailDto;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.model.GradeModel;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.ArtifactRow;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.RosterRow;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.StatusCount;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.SubmissionRow;
import org.ssglobal.revalida.ssclassroom.service.ReviewService;
import org.ssglobal.revalida.ssclassroom.service.S3Bucket.S3Service;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final String ASSIGNMENT = "asg_TEST";
    private static final String SUBMISSION = "sub_TEST";
    private static final String LEARNER = "usr_LEARNER";
    private static final Instant SUBMITTED_AT = Instant.parse("2026-11-12T06:30:00Z");

    @Mock GradeRepository gradeRepository;
    @Mock S3Service s3Service;

    ReviewService reviewService;

    record TestRosterRow(String getSubmissionId, String getLearnerId, String getFirstName,
            String getLastName, String getStatus, Integer getPoints, Instant getSubmittedAt)
            implements RosterRow {
    }

    record TestStatusCount(String getStatus, Long getTotal) implements StatusCount {
    }

    record TestSubmissionRow(String getSubmissionId, String getAssignmentId, String getLearnerId,
            String getFirstName, String getLastName, String getStatus, Instant getSubmittedAt)
            implements SubmissionRow {
    }

    record TestArtifactRow(String getKind, String getName, String getBody, String getUrl, Long getSize, String getS3Key)
            implements ArtifactRow {
    }

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(gradeRepository, s3Service);
    }

    private void givenSubmission() {
        TestSubmissionRow row = new TestSubmissionRow(SUBMISSION, ASSIGNMENT, LEARNER,
                "Juan", "Dela Cruz", "submitted", SUBMITTED_AT);
        when(gradeRepository.findSubmissionDetail(SUBMISSION)).thenReturn(Optional.of(row));
    }

    @Test
    void rosterThrowsNotFoundWhenAssignmentDoesNotExist() {
        when(gradeRepository.assignmentExists(ASSIGNMENT)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getRoster(ASSIGNMENT, null, 50, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rosterBuildsItemsSummaryAndNextCursor() {
        TestRosterRow first = new TestRosterRow("sub_A", "usr_A", "Ana", "Reyes", "submitted", null, SUBMITTED_AT);
        TestRosterRow second = new TestRosterRow("sub_B", "usr_B", "Ben", "Cruz", "returned", 90, SUBMITTED_AT);
        TestRosterRow extra = new TestRosterRow("sub_C", "usr_C", "Cara", "Lim", "assigned", null, null);

        when(gradeRepository.assignmentExists(ASSIGNMENT)).thenReturn(true);
        when(gradeRepository.findRosterPage(ASSIGNMENT, null, null, 3)).thenReturn(List.of(first, second, extra));
        when(gradeRepository.countByStatus(ASSIGNMENT)).thenReturn(List.of(
                new TestStatusCount("assigned", 1L),
                new TestStatusCount("submitted", 1L),
                new TestStatusCount("returned", 1L)));

        RosterResponseDto roster = reviewService.getRoster(ASSIGNMENT, null, 2, null);

        assertThat(roster.items()).hasSize(2);
        assertThat(roster.items().get(0).learnerLabel()).isEqualTo("Ana Reyes");
        assertThat(roster.items().get(0).submittedAt()).isEqualTo(SUBMITTED_AT);
        assertThat(roster.items().get(1).points()).isEqualTo(90);
        assertThat(roster.nextCursor()).isEqualTo("sub_B");
        assertThat(roster.summary().total()).isEqualTo(3);
    }

    @Test
    void submissionThrowsNotFoundWhenLearnerOpensSomeoneElsesWork() {
        givenSubmission();

        assertThatThrownBy(() -> reviewService.getSubmission(SUBMISSION, "usr_OTHER", false))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(s3Service);
    }

    @Test
    void submissionForTrainerHasNameDownloadLinkAndGrade() {
        givenSubmission();
        TestArtifactRow artifact = new TestArtifactRow(
                "file", "essay.pdf", null,"", 0L, "key/essay.pdf");
        PresignedS3UrlDto presigned = new PresignedS3UrlDto("https://s3/essay.pdf", "key/essay.pdf", "GET", "pdf-bucket", 0);
        GradeModel grade = new GradeModel(SUBMISSION, 95, "Nice", SUBMITTED_AT, "usr_TRAINER");

        when(gradeRepository.findArtifact(SUBMISSION)).thenReturn(Optional.of(artifact));
        when(s3Service.resolveBucketForFile("pdf")).thenReturn("pdf-bucket");
        when(s3Service.createPresignedGetUrl("pdf-bucket", "key/essay.pdf")).thenReturn(presigned);
        when(gradeRepository.findById(SUBMISSION)).thenReturn(Optional.of(grade));

        SubmissionDetailDto detail = reviewService.getSubmission(SUBMISSION, "usr_TRAINER", true);

        assertThat(detail.learnerLabel()).isEqualTo("Juan Dela Cruz");
        assertThat(detail.artifacts()).singleElement()
                .extracting(SubmissionDetailDto.Artifact::downloadUrl)
                .isEqualTo("https://s3/essay.pdf");
        assertThat(detail.grade().points()).isEqualTo(95);
    }

    @Test
    void submissionWithTextArtifactDoesNotCallS3() {
        givenSubmission();
        TestArtifactRow artifact = new TestArtifactRow(
                "text", null,  "My answer", "", 0L, null);

        when(gradeRepository.findArtifact(SUBMISSION)).thenReturn(Optional.of(artifact));
        when(gradeRepository.findById(SUBMISSION)).thenReturn(Optional.empty());

        SubmissionDetailDto detail = reviewService.getSubmission(SUBMISSION, LEARNER, false);

        assertThat(detail.artifacts()).singleElement()
                .extracting(SubmissionDetailDto.Artifact::body)
                .isEqualTo("My answer");
        assertThat(detail.grade()).isNull();
        verify(s3Service, never()).createPresignedGetUrl(any(), any());
    }
}