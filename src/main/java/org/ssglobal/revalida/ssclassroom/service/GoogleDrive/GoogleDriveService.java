package org.ssglobal.revalida.ssclassroom.service.GoogleDrive;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ssglobal.revalida.ssclassroom.dto.UploadSubmission.UploadSubmissionRequestDto;
import org.ssglobal.revalida.ssclassroom.enums.SubmissionKind;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleDriveService {

  private final Drive driveService;

  public UploadSubmissionRequestDto extractFile(String driveUrl) {
    log.info("Extract files from Google Drive...");
    if (driveUrl == null || driveUrl.isBlank()){
      throw new IllegalArgumentException("Drive URL cannot be null or empty.");
    }

    try {
      // Can only handle one file for now. Multiple Files will be handled in the future.
      if (driveUrl.contains("/folders/")){
        String folderId = extractId(driveUrl, "/folders/");
        return listFilesInFolder(folderId).stream()
                .findFirst()
                .map(object -> {
                  UploadSubmissionRequestDto requestDto =
                          new UploadSubmissionRequestDto();

                  requestDto.setKind(SubmissionKind.file);
                  requestDto.setFilename(object.getName());
                  requestDto.setContentType(object.getMimeType());
                  requestDto.setSizeBytes(object.getSize());
                  return requestDto;

                }).orElseThrow(() ->
                        new NotFoundException("The Google Drive folder is empty."));
      }


      if (driveUrl.contains("/file/d/")) {
        String fileId = extractId(driveUrl, "/file/d/");
        File object = getSingleFile(fileId);

        UploadSubmissionRequestDto requestDto = new UploadSubmissionRequestDto();
        requestDto.setKind(SubmissionKind.file);
        requestDto.setFilename(object.getName());
        requestDto.setContentType(object.getMimeType());
        requestDto.setSizeBytes(object.getSize());
        return requestDto;
      }

      throw new IllegalArgumentException("Invalid Google Drive URL format.");

    } catch (GoogleJsonResponseException ex) {
      // Handle specific Google API errors (e.g., 404 Not Found, 403 Forbidden)
      log.error("Google Drive API error [Status {}]: {}",
              ex.getStatusCode(),
              ex.getDetails().getMessage()
      );

      if (ex.getStatusCode() == 404) {
        throw NotFoundException.of(
                "Google Drive File/Folder",
                "Resource not found or does not exist."
        );
      } else if (ex.getStatusCode() == 403) {
        throw new RuntimeException("Access denied. Ensure the Google Drive file " +
                "or folder is shared with the application credentials.");
      }

      throw new RuntimeException("Google Drive API failed: " + ex.getDetails().getMessage(), ex);

    }catch (Exception ex){
      log.error("Failed to extract files from Google Drive URL: {}", driveUrl, ex);
      throw new RuntimeException("Google Drive extraction failed: " + ex.getMessage(), ex);
    }

  }
  //

  private String extractId(String url, String delimiter) {
    String[] parts = url.split(delimiter);
    if (parts.length > 1) {
      return parts[1].split("/")[0].split("\\?")[0];
    }
    throw new IllegalArgumentException("Could not parse ID from URL using delimiter: " + delimiter);
  }

  private List<File> listFilesInFolder(String folderId) throws IOException {
    String query = "'" + folderId + "' in parents and trashed = false";

    FileList result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name, mimeType, webViewLink, webContentLink, size)")
            .execute();

    return result.getFiles() != null ? result.getFiles().stream().toList() : List.of();
  }

  private File getSingleFile(String fileId) throws IOException {
    return driveService.files().get(fileId)
            .setFields("id, name, mimeType, webViewLink, webContentLink, iconLink, size")
            .execute();
  }
}
