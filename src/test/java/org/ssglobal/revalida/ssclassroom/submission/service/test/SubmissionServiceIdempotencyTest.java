package org.ssglobal.revalida.ssclassroom.submission.service.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.Status;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionType;
import org.ssglobal.revalida.ssclassroom.exceptions.StateInvalidException;
import org.ssglobal.revalida.ssclassroom.model.*;
import org.ssglobal.revalida.ssclassroom.repository.IdempotencyKeyRepository;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionArtifactRepository;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionRepository;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class SubmissionServiceIdempotencyTest {

  @Mock
  private SubmissionRepository submissionRepository;
  @Mock
  private SubmissionArtifactRepository submissionArtifactRepository;
  @Mock
  private IdempotencyKeyRepository idempotencyKeyRepository;

  @InjectMocks
  private SubmissionService submissionService;

  private SubmissionModel mockSubmission;
  private TurnInSubmissionRequestDto requestDto;

  @BeforeEach
  void setUp() {
    mockSubmission = new SubmissionModel();
    // Setup base submission configuration...

    SubmissionIdCompositeModel compositeId = new SubmissionIdCompositeModel();
    compositeId.setSubmissionId("sub_123");
    compositeId.setLearnerId("usr_01M7J62VNN07XR1T076JSK061Y"); // Must match the test's learnerId!
    mockSubmission.setSubmissionCompositeId(compositeId);
    mockSubmission.setStatus(Status.assigned);

    requestDto = new TurnInSubmissionRequestDto();
    requestDto.setKind(SubmissionType.text);
    requestDto.setBody("Sample student answer text");
  }

  @Test
  void turnInSubmission_WithNewIdempotencyKey_ShouldSucceed() {
    String submissionId = "sub_123";
    String learnerId = "usr_01M7J62VNN07XR1T076JSK061Y";
    String idempotencyKey = "key_unique_abc";

    when(idempotencyKeyRepository.findByIdemKey(idempotencyKey))
            .thenReturn(Optional.empty()); // Key has never been used

    when(submissionRepository.findBySubmissionId(submissionId))
            .thenReturn(Optional.of(mockSubmission));

    TurnInSubmissionResponseDto response = submissionService.turnInSubmission(
            submissionId, learnerId, idempotencyKey, requestDto
    );

    assertNotNull(response);

    verify(idempotencyKeyRepository, times(1)).save(any(IdempotencyKeyModel.class));
  }

  @Test
  void turnInSubmission_WithDuplicateIdempotencyKey_ShouldThrowException() {
    String submissionId = "sub_123";
    String learnerId = "user_456";
    String idempotencyKey = "key_already_used";

    IdempotencyKeyModel existingRecord = new IdempotencyKeyModel();
    IdempotencyCompositeModel compositeId = new IdempotencyCompositeModel();
    compositeId.setIdemKey(idempotencyKey);
    compositeId.setUserId(learnerId);
    compositeId.setSubmissionId(submissionId);
    existingRecord.setIdempotencyCompositeModel(compositeId);


    when(idempotencyKeyRepository.findByIdemKey(idempotencyKey))
            .thenReturn(Optional.of(existingRecord));

    when(submissionRepository.findBySubmissionId(submissionId))
            .thenReturn(Optional.of(mockSubmission));

    SubmissionArtifactModel mockArtifact = new SubmissionArtifactModel();
    mockArtifact.setKind(SubmissionType.text);
    when(submissionArtifactRepository.findBySubmissionId(submissionId))
            .thenReturn(Optional.of(mockArtifact));


    TurnInSubmissionResponseDto response = submissionService.turnInSubmission(
            submissionId, learnerId, idempotencyKey, requestDto
    );

    assertNotNull(response);
  }

  @Test
  void turnInSubmission_WithMismatchedUserOrSubmission_ShouldThrowException() {
    String submissionId = "sub_123";
    String learnerId = "user_456";
    String idempotencyKey = "key_hijacked";

    //  key belongs to a DIFFERENT submission
    IdempotencyKeyModel foreignRecord = new IdempotencyKeyModel();

    IdempotencyCompositeModel compositeId = new IdempotencyCompositeModel();
    compositeId.setIdemKey(idempotencyKey);
    compositeId.setUserId("different_user");
    compositeId.setSubmissionId("different_sub");

    foreignRecord.setIdempotencyCompositeModel(compositeId);

    when(idempotencyKeyRepository.findByIdemKey(idempotencyKey))
            .thenReturn(Optional.of(foreignRecord));

    //  Should block cross-user/cross-submission key hijacking
    assertThrows(
            StateInvalidException.class,
            () -> submissionService.turnInSubmission(submissionId, learnerId, idempotencyKey, requestDto)
    );
  }
}