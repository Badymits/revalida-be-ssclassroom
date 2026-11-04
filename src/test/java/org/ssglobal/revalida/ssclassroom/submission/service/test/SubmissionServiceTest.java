package org.ssglobal.revalida.ssclassroom.submission.service.test;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;
import org.ssglobal.revalida.ssclassroom.exceptions.PayloadTooLargeException;
import org.ssglobal.revalida.ssclassroom.service.S3Service.S3Service;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SubmissionServiceTest {

  @Mock
  private S3Service s3Service;

  @InjectMocks
  private SubmissionService submissionService;

  @Test
  public void testUploadFileSizeTooLarge() {
    long fiftyMegaBytes = 51L * 1024 * 1024;

    UploadSubmissionRequestDto requestDto = new UploadSubmissionRequestDto();
    requestDto.setSizeBytes((int) fiftyMegaBytes);

    PayloadTooLargeException exception = assertThrows(
            PayloadTooLargeException.class,
            () -> submissionService.uploadSubmission(requestDto),
            "File size too large!"
    );

    assertEquals(ErrorCode.PAYLOAD_TOO_LARGE.getStatus(), exception.getCode().getStatus());
  }
}
