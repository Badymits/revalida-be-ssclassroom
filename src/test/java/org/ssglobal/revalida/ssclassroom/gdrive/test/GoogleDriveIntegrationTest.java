package org.ssglobal.revalida.ssclassroom.gdrive.test;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.service.GoogleDrive.GoogleDriveService;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Real Google Drive calls. Each test is skipped unless its env variable is set.
@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class GoogleDriveIntegrationTest {

  private static final String FOLDER_MIME = "application/vnd.google-apps.folder";

  @Autowired
  private GoogleDriveService googleDriveService;

  @Autowired
  private Drive drive;

  private final String fileUrl = "https://drive.google.com/file/d/1Pw-WI6oEWPN4PcOefSNvMKjLSse2KRRx/view?usp=sharing";
  private final String folderUrl = "https://drive.google.com/drive/folders/1FIMpLEjP2b9hUcJ5MyKlMRyrsGfTjZoM?usp=drive_link";


  @Test
  @EnabledIfEnvironmentVariable(named = "DRIVE_TEST_FILE_URL", matches = ".+")
  void file_returnsRealMetadata() {
    File dto = googleDriveService.extractFile(fileUrl);

    log.info(dto.toString());
    assertNotNull(dto.getName());
    assertFalse(dto.getName().isBlank());
    assertNotNull(dto.getMimeType());
    assertNotNull(dto.getSize(), "size is null (Google Docs/Sheets have no size)");
    assertTrue(dto.getSize() > 0);
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "DRIVE_TEST_FILE_URL", matches = ".+")
  void file_downloadsRealBytes() throws Exception {
    String fileId = idFrom(fileUrl, "/file/d/");
    File dto = googleDriveService.extractFile(fileUrl);
    log.info(dto.toString());
    try (InputStream in = drive.files().get(fileId).executeMediaAsInputStream()) {
      assertEquals(dto.getSize(), (long) in.readAllBytes().length);
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "DRIVE_TEST_FOLDER_URL", matches = ".+")
  void folder_returnsFileFromFolder() throws Exception {
    List<File> children = listFolder(idFrom(folderUrl, "/folders/"));
    assertFalse(children.isEmpty(), "test folder is empty, add at least one file");

    File dto = googleDriveService.extractFile(folderUrl);

    assertTrue(
            children.stream().anyMatch(f -> f.getName().equals(dto.getName())),
            "extractFile returned '" + dto.getName() + "', which is not in the folder");
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "DRIVE_TEST_FOLDER_URL", matches = ".+")
  void folder_pickedItemIsNotASubfolder() {
    File dto = googleDriveService.extractFile(folderUrl);


    assertNotEquals(FOLDER_MIME, dto.getMimeType(), "picked a subfolder");
    assertNotNull(dto.getSize(), "picked an item with no size (subfolder or Google Doc)");
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "DRIVE_TEST_FOLDER_URL", matches = ".+")
  void folder_pickedFileDownloads() throws Exception {
    File dto = googleDriveService.extractFile(folderUrl);

    File picked = listFolder(idFrom(folderUrl, "/folders/")).stream()
            .filter(f -> f.getName().equals(dto.getName()))
            .findFirst()
            .orElseThrow();

    try (InputStream in = drive.files().get(picked.getId()).executeMediaAsInputStream()) {
      assertEquals(dto.getSize(), (long) in.readAllBytes().length);
    }
  }

  @Test
  void invalidUrl_throws() {
    assertThrows(RuntimeException.class,
            () -> googleDriveService.extractFile("https://example.com/not-drive"));
  }

  private List<File> listFolder(String folderId) throws Exception {
    return drive.files().list()
            .setQ("'" + folderId + "' in parents and trashed = false")
            .setFields("files(id, name, mimeType, size)")
            .execute()
            .getFiles();
  }

  private String idFrom(String url, String delimiter) {
    return url.split(delimiter)[1].split("/")[0].split("\\?")[0];
  }
}