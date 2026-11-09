package org.ssglobal.revalida.ssclassroom.submission.service.test;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;
import org.ssglobal.revalida.ssclassroom.enums.Status;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;
import org.ssglobal.revalida.ssclassroom.exceptions.InvalidFileException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.exceptions.PayloadTooLargeException;
import org.ssglobal.revalida.ssclassroom.exceptions.StateInvalidException;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionRepository;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubmissionServiceTest {

  @Mock
  private SubmissionRepository submissionRepository;

  @InjectMocks
  private SubmissionService submissionService;

  @Test
  public void testUploadFileSizeTooLarge() {
    long fiftyMegaBytes = 51L * 1024 * 1024;

    UploadSubmissionRequestDto requestDto = new UploadSubmissionRequestDto();
    requestDto.setSizeBytes((int) fiftyMegaBytes);

    PayloadTooLargeException exception = assertThrows(
            PayloadTooLargeException.class,
            () -> submissionService.generateUploadUrlSubmission(requestDto),
            "File size too large!"
    );

    assertEquals(ErrorCode.PAYLOAD_TOO_LARGE.getStatus(), exception.getCode().getStatus());
  }

  @Test
  public void testTurnInSubmission_Success(){

    String submissionId = "sub_0001";
    TurnInSubmissionRequestDto dto = new TurnInSubmissionRequestDto();

    dto.setArtifactId("artifact_001");
    dto.setKind(SubmissionKind.FILE);

    SubmissionModel model = new SubmissionModel();
    model.setStatus(Status.ASSIGNED);

    when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(model));
    when(submissionRepository.save(any(SubmissionModel.class))).thenReturn(model);

    submissionService.updateSubmissionTurnIn(submissionId, dto);

    assertEquals(Status.SUBMITTED, model.getStatus());

    verify(submissionRepository, times(1)).findById(submissionId);
    verify(submissionRepository, times(1)).save(model);

  }

  @Test
  public void testUnsubmitSubmission_Success() {

    String submissionId = "sub_0001";
    SubmissionModel existingModel = new SubmissionModel();

    existingModel.setStatus(Status.SUBMITTED);

    when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(existingModel));
    when(submissionRepository.save(any(SubmissionModel.class))).thenReturn(existingModel);

   submissionService.unsubmitSubmission(submissionId);

   assertEquals(Status.ASSIGNED, existingModel.getStatus());

    verify(submissionRepository, times(1)).findById(submissionId);
    verify(submissionRepository, times(1)).save(existingModel);
  }

  @Test
  public void testRetrieveFileType_TextSunny(){
    String file = "dummy.txt";
    String res = submissionService.retrieveFileExtension(file);

    assertEquals("txt", res);
  }

  // test to retrieve last file extension after multiple dots
  @Test
  public void testRetrieveFileType_TextSunny_MultipleDots(){
    String file = "my.dummy.txt.pdf";
    String res = submissionService.retrieveFileExtension(file);

    assertEquals("pdf", res);
  }

  @Test
  public void testRetrieveFileType_TextRainyOne(){
    String file = "dummy.txt";
    String res = submissionService.retrieveFileExtension(file);

    assertNotEquals("pdf", res);
  }

  // testing inputs with no file extension
  @Test
  public void testRetrieveFileType_TextRainyTwo(){
    String file = "dummy";

    assertThrows(StringIndexOutOfBoundsException.class, () -> {
      submissionService.retrieveFileExtension(file);
    });
  }



  @Test
  public void testUnsubmitSubmission_IdNotFound() {
    String submissionId = "sub_01234";

    when(submissionRepository.findById(submissionId))
            .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class,
            () -> submissionService.unsubmitSubmission(submissionId));
  }

  @Test
  /*
    Invalid States in unsubmitting a submission: Assigned OR Returned
   */
  public void testUnsubmitSubmission_InvalidState(){
    String submissionId = "sub_0001";

    SubmissionModel existingModel = new SubmissionModel();
    existingModel.setStatus(Status.RETURNED);

    when(submissionRepository.findById(submissionId))
            .thenReturn(Optional.of(existingModel));

    assertThrows(StateInvalidException.class,
            () -> submissionService.unsubmitSubmission(submissionId));
  }


}
