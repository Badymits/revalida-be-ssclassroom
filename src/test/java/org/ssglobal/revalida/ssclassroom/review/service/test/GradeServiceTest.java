package org.ssglobal.revalida.ssclassroom.review.service.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ssglobal.revalida.ssclassroom.exceptions.BadRequestException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.exceptions.StateInvalidException;
import org.ssglobal.revalida.ssclassroom.model.GradeModel;
import org.ssglobal.revalida.ssclassroom.repository.GradeDraftRepository;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository;
import org.ssglobal.revalida.ssclassroom.repository.GradeRepository.SubmissionForReturn;
import org.ssglobal.revalida.ssclassroom.service.GradeService;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    private static final String SUBMISSION = "sub_TEST";
    private static final String TRAINER = "usr_TRAINER";

    @Mock GradeRepository gradeRepository;
    @Mock GradeDraftRepository gradeDraftRepository;

    GradeService gradeService;

    record TestSubmission(String getStatus, Integer getPoints) implements SubmissionForReturn {
    }

    @BeforeEach
    void setUp() {
        gradeService = new GradeService(gradeRepository, gradeDraftRepository);
    }

    private void givenSubmission(String status, int maxPoints) {
        TestSubmission submission = new TestSubmission(status, maxPoints);
        when(gradeRepository.findSubmissionForReturn(SUBMISSION)).thenReturn(Optional.of(submission));
    }

    @Test
    void returnsAndSavesTheGrade() {
        givenSubmission("submitted", 100);
        when(gradeRepository.markReturned(SUBMISSION)).thenReturn(1);

        GradeModel grade = gradeService.returnSubmission(SUBMISSION, 85, "Good work", TRAINER);

        assertThat(grade.getSubmissionId()).isEqualTo(SUBMISSION);
        assertThat(grade.getPoints()).isEqualTo(85);
        assertThat(grade.getPrivateComment()).isEqualTo("Good work");
        assertThat(grade.getReturnedBy()).isEqualTo(TRAINER);
        assertThat(grade.getReturnedAt()).isNotNull();
        verify(gradeRepository).save(grade);
        verify(gradeDraftRepository).deleteById(SUBMISSION);
    }

    @Test
    void savesAnEmptyCommentWhenCommentIsNull() {
        givenSubmission("submitted", 100);
        when(gradeRepository.markReturned(SUBMISSION)).thenReturn(1);

        GradeModel grade = gradeService.returnSubmission(SUBMISSION, 70, null, TRAINER);

        assertThat(grade.getPrivateComment()).isEmpty();
    }

    @Test
    void allowsPointsEqualToTheMaximum() {
        givenSubmission("submitted", 100);
        when(gradeRepository.markReturned(SUBMISSION)).thenReturn(1);

        GradeModel grade = gradeService.returnSubmission(SUBMISSION, 100, "", TRAINER);

        assertThat(grade.getPoints()).isEqualTo(100);
    }

    @Test
    void throwsNotFoundWhenSubmissionDoesNotExist() {
        when(gradeRepository.findSubmissionForReturn(SUBMISSION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gradeService.returnSubmission(SUBMISSION, 80, "", TRAINER))
                .isInstanceOf(NotFoundException.class);
        verify(gradeRepository, never()).save(any());
    }

    @Test
    void throwsStateInvalidWhenSubmissionIsOnlyAssigned() {
        givenSubmission("assigned", 100);

        assertThatThrownBy(() -> gradeService.returnSubmission(SUBMISSION, 80, "", TRAINER))
                .isInstanceOf(StateInvalidException.class);
        verify(gradeRepository, never()).markReturned(any());
        verify(gradeRepository, never()).save(any());
    }

    @Test
    void throwsStateInvalidWhenSubmissionIsAlreadyGraded() {
        givenSubmission("returned", 100);

        assertThatThrownBy(() -> gradeService.returnSubmission(SUBMISSION, 80, "", TRAINER))
                .isInstanceOf(StateInvalidException.class);
        verify(gradeRepository, never()).save(any());
    }

    @Test
    void throwsBadRequestOnPointsWhenPointsAreAboveTheMaximum() {
        givenSubmission("submitted", 100);

        assertThatThrownBy(() -> gradeService.returnSubmission(SUBMISSION, 101, "", TRAINER))
                .isInstanceOf(BadRequestException.class)
                .satisfies(error -> assertThat(((BadRequestException) error).getDetails())
                        .extracting("field")
                        .containsExactly("points"));
        verify(gradeRepository, never()).markReturned(any());
        verify(gradeRepository, never()).save(any());
    }

    @Test
    void throwsStateInvalidWhenAnotherTrainerGradedItFirst() {
        givenSubmission("submitted", 100);
        when(gradeRepository.markReturned(SUBMISSION)).thenReturn(0);

        assertThatThrownBy(() -> gradeService.returnSubmission(SUBMISSION, 80, "", TRAINER))
                .isInstanceOf(StateInvalidException.class);
        verify(gradeRepository, never()).save(any());
        verify(gradeDraftRepository, never()).deleteById(any());
    }
}