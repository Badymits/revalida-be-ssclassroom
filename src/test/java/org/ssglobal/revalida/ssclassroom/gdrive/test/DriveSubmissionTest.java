package org.ssglobal.revalida.ssclassroom.gdrive.test;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.ssglobal.revalida.ssclassroom.controller.SubmissionController;
import org.ssglobal.revalida.ssclassroom.dto.ArtifactDto;
import org.ssglobal.revalida.ssclassroom.dto.TurnInSubmission.TurnInSubmissionResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.Status;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionType;
import org.ssglobal.revalida.ssclassroom.error.GlobalExceptionHandler;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.service.DriveSubmissionProcessService;

import java.security.Principal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(MockitoExtension.class)
public class DriveSubmissionTest {

  private MockMvc mockMvc;

  @Mock
  private DriveSubmissionProcessService driveSubmissionService;

  @InjectMocks
  private SubmissionController submissionController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(submissionController).build();

    mockMvc = MockMvcBuilders.standaloneSetup(submissionController)
            .setControllerAdvice(new GlobalExceptionHandler()) // rest controller advice
            .build();
  }

  @Test
  void testProcessDriveSubmission_SingleFile_Success() throws Exception {
    String submissionId = "sub_test01sub";
    String driveUrl = "https://drive.google.com/file/d/1Pw-WI6oEWPN4PcOefSNvMKjLSse2KRRx/view?usp=drive_link";
    String learnerId = "usr_01M7J62VNN07XR1T076JSK061Y";
    Principal mockPrincipal = () -> learnerId;

    TurnInSubmissionResponseDto mockResponse = new TurnInSubmissionResponseDto();
    mockResponse.setSubmissionId(submissionId);
    mockResponse.setStatus(Status.submitted);
    mockResponse.setSubmittedAt(Instant.now());

    ArtifactDto artifactDto = new ArtifactDto();
    artifactDto.setKind(SubmissionType.file);
    artifactDto.setName("Use cases of Kutob at Bulong.txt");
    artifactDto.setSize(997L);
    artifactDto.setUrl("https://s3.amazonaws.com/txt-bucket/mock-presigned-url");
    mockResponse.setArtifact(artifactDto);

    when(driveSubmissionService.processDriveSubmission(eq(submissionId), eq(driveUrl),
            eq(learnerId), nullable(String.class)))
            .thenReturn(mockResponse);

    mockMvc.perform(post("/api/v1/submissions/{submissionId}/process-files-from-drive", submissionId)
                    .principal(mockPrincipal)
                    .content(driveUrl)
                    .header("Idempotency-Key", "idem-123")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect((ResultMatcher) jsonPath("$.submissionId").value(submissionId))
            .andExpect((ResultMatcher) jsonPath("$.status").value("submitted"))
            .andExpect((ResultMatcher) jsonPath("$.artifact.name")
                    .value("Use cases of Kutob at Bulong.txt"))
            .andExpect((ResultMatcher) jsonPath("$.artifact.size").value(997))
            .andExpect((ResultMatcher) jsonPath("$.artifact.url").exists());
  }

  @Test
  void testProcessDriveSubmission_Folder_Success() throws Exception {
    String submissionId = "sub_test01sub";
    String driveUrl = "https://drive.google.com/drive/folders/1FIMpLEjP2b9hUcJ5MyKlMRyrsGfTjZoM?usp=drive_link";
    String learnerId = "usr_01M7J62VNN07XR1T076JSK061Y";
    Principal mockPrincipal = () -> learnerId;

    TurnInSubmissionResponseDto mockResponse = new TurnInSubmissionResponseDto();
    mockResponse.setSubmissionId(submissionId);
    mockResponse.setStatus(Status.submitted);
    mockResponse.setSubmittedAt(Instant.now());

    ArtifactDto artifactDto = new ArtifactDto();
    artifactDto.setKind(SubmissionType.file);
    artifactDto.setName("Use cases of Kutob at Bulong.txt");
    artifactDto.setSize(997L);
    artifactDto.setUrl("https://s3.amazonaws.com/txt-bucket/mock-presigned-url");
    mockResponse.setArtifact(artifactDto);

    when(driveSubmissionService.processDriveSubmission(eq(submissionId), eq(driveUrl),
            eq(learnerId), nullable(String.class)))
            .thenReturn(mockResponse);

    mockMvc.perform(post("/api/v1/submissions/{submissionId}/process-files-from-drive",
                    submissionId).content(driveUrl).principal(mockPrincipal)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect((ResultMatcher) jsonPath("$.submissionId").value(submissionId))
            .andExpect((ResultMatcher) jsonPath("$.status").value("submitted"))
            .andExpect((ResultMatcher) jsonPath("$.artifact.name")
                    .value("Use cases of Kutob at Bulong.txt"));
  }

  @Test
  void testProcessDriveSubmission_NotFound() throws Exception {
    String submissionId = "non-existent-sub";
    String driveUrl = "https://drive.google.com/file/d/1Pw-WI6oEWPN4PcOefSNvMKjLSse2KRRx/view?usp=drive_link";
    String learnerId = "usr_01M7J62VNN07XR1T076JSK061Y";
    Principal mockPrincipal = () -> learnerId;

    when(driveSubmissionService.processDriveSubmission(
            eq(submissionId), eq(driveUrl),
            eq(learnerId), nullable(String.class)
            ))
            .thenThrow(NotFoundException.of("Submission", submissionId));

    mockMvc.perform(post("/api/v1/submissions/{submissionId}/process-files-from-drive", submissionId)
                    .content(driveUrl)
                    .principal(mockPrincipal)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
  }
}
