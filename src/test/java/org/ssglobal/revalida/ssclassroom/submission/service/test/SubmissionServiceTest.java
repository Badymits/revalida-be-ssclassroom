package org.ssglobal.revalida.ssclassroom.submission.service.test;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;
import org.ssglobal.revalida.ssclassroom.enums.Status;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionType;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.exceptions.PayloadTooLargeException;
import org.ssglobal.revalida.ssclassroom.exceptions.StateInvalidException;
import org.ssglobal.revalida.ssclassroom.model.SubmissionArtifactModel;
import org.ssglobal.revalida.ssclassroom.model.SubmissionIdCompositeModel;
import org.ssglobal.revalida.ssclassroom.model.SubmissionModel;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionArtifactRepository;
import org.ssglobal.revalida.ssclassroom.repository.SubmissionRepository;
import org.ssglobal.revalida.ssclassroom.service.S3Bucket.S3Service;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubmissionServiceTest {

  @Mock
  private SubmissionRepository submissionRepository;

  @Mock
  private SubmissionArtifactRepository submissionArtifactRepository;

  @Mock
  private S3Service s3Service;

  @InjectMocks
  private SubmissionService submissionService;

  @Test
  public void testUploadFileSizeTooLarge() {
    long fiftyMegaBytes = 51L * 1024 * 1024;

    UploadSubmissionRequestDto requestDto = new UploadSubmissionRequestDto();
    requestDto.setSizeBytes(fiftyMegaBytes);

    PayloadTooLargeException exception = assertThrows(
            PayloadTooLargeException.class,
            () -> submissionService.generateS3UploadUrl("", "", requestDto, ""),
            "File size too large!"
    );

    assertEquals(ErrorCode.PAYLOAD_TOO_LARGE.getStatus(), exception.getCode().getStatus());
  }

  @Test
  public void testTurnInSubmission_Success() {
    String submissionId = "sub_0001";
    String learnerId = "usr_0001";
    String assignmentId = "arg_0001";

    TurnInSubmissionRequestDto dto = new TurnInSubmissionRequestDto();
    dto.setArtifactId("artifact_001");
    dto.setKind(SubmissionType.file);

    SubmissionIdCompositeModel compositeId = new SubmissionIdCompositeModel();
    compositeId.setLearnerId(learnerId);
    compositeId.setSubmissionId(submissionId);
    compositeId.setAssignmentId(assignmentId);

    SubmissionModel model = new SubmissionModel();
    model.setSubmissionCompositeId(compositeId);
    model.setStatus(Status.assigned);

    SubmissionArtifactModel artifact = new SubmissionArtifactModel();
    artifact.setArtifactName("Sample.txt");
    artifact.setContentType("text/plain");
    artifact.setKind(SubmissionType.file);

    PresignedS3UrlDto mockPresignedUrl = PresignedS3UrlDto.builder()
            .uploadUrl("https://s3.amazonaws.com/txt-submissions-bucket/Sample.txt")
            .s3Key("submissions/sub_0001/Sample.txt")
            .httpMethod("GET")
            .bucketName("txt-submissions-bucket")
            .expiresIn(3600)
            .build();

    when(submissionRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(model));
    when(submissionRepository.save(any(SubmissionModel.class))).thenReturn(model);
    when(submissionArtifactRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(artifact));
    when(s3Service.resolveBucketForFile("txt")).thenReturn("txt-submissions-bucket");
    when(s3Service.createPresignedGetUrl(eq("txt-submissions-bucket"), nullable(String.class)))
            .thenReturn(mockPresignedUrl);

    submissionService.turnInSubmission(submissionId, learnerId, "", dto);

    assertEquals(Status.submitted, model.getStatus());
    verify(submissionRepository, times(1)).findBySubmissionId(submissionId);
    verify(submissionRepository, times(1)).save(model);
  }

  @Test
  public void testUnsubmitSubmission_Success() {

    String submissionId = "sub_0001";
    String learnerId = "usr_0001";

    TurnInSubmissionRequestDto dto = new TurnInSubmissionRequestDto();
    dto.setArtifactId("artifact_001");
    dto.setKind(SubmissionType.file);

    SubmissionIdCompositeModel compositeId = new SubmissionIdCompositeModel();
    compositeId.setLearnerId(learnerId);

    SubmissionModel model = new SubmissionModel();
    model.setSubmissionCompositeId(compositeId);
    model.setStatus(Status.assigned);

    SubmissionArtifactModel artifact = new SubmissionArtifactModel();
    artifact.setArtifactName("Sample.txt");
    artifact.setContentType("text/plain");
    artifact.setKind(SubmissionType.file);


    when(submissionRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(model));
    when(submissionRepository.save(any(SubmissionModel.class))).thenReturn(model);
    when(submissionArtifactRepository.findBySubmissionId(submissionId))
            .thenReturn(Optional.of(artifact));

    when(s3Service.resolveBucketForFile("txt")).thenReturn("txt-submissions-bucket");
    PresignedS3UrlDto mockPresignedUrl = PresignedS3UrlDto.builder()
            .uploadUrl("https://s3.amazonaws.com/txt-submissions-bucket/Sample.txt")
            .s3Key("submissions/sub_0001/Sample.txt")
            .httpMethod("GET")
            .bucketName("txt-submissions-bucket")
            .expiresIn(3600)
            .build();
    when(s3Service.createPresignedGetUrl(eq("txt-submissions-bucket"), nullable(String.class)))
            .thenReturn(mockPresignedUrl);

    submissionService.turnInSubmission(submissionId, learnerId, "", dto);

    assertEquals(Status.submitted, model.getStatus());
    verify(submissionRepository, times(1)).findBySubmissionId(submissionId);
    verify(submissionRepository, times(1)).save(model);
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

    assertThrows(StringIndexOutOfBoundsException.class,
            () -> submissionService.retrieveFileExtension(file));
  }


  @Test
  public void testUnsubmitSubmission_IdNotFound() {
    String submissionId = "sub_01234";
    String learnerIdStub = "";

    when(submissionRepository.findBySubmissionId(submissionId))
            .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class,
            () -> submissionService.unsubmitSubmission(submissionId, learnerIdStub));
  }

  @Test
  /*
    Invalid States in unsubmitting a submission: Assigned OR Returned
   */
  public void testUnsubmitSubmission_InvalidState(){
    String submissionId = "sub_0001";
    String learnerIdStub = "";

    SubmissionModel existingModel = new SubmissionModel();
    existingModel.setStatus(Status.returned);

    when(submissionRepository.findBySubmissionId(submissionId))
            .thenReturn(Optional.of(existingModel));

    assertThrows(StateInvalidException.class,
            () -> submissionService.unsubmitSubmission(submissionId, learnerIdStub));
  }


}
