package org.ssglobal.revalida.ssclassroom.service.GoogleDrive;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;
import org.ssglobal.revalida.ssclassroom.exceptions.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleDriveService {

  private final Drive driveService;

  public File extractFile(String driveUrl) {
    log.info("Extract files from Google Drive...");
    if (driveUrl == null || driveUrl.isBlank()){
      throw new ApiException(
              ErrorCode.NOT_FOUND,
              "Drive URL cannot be null or empty.");
    }

    try {
      // Handles folder read and file extraction if user shares link of folder
      // can only handle 1 file
      if (driveUrl.contains("/folders/")){
        String folderId = extractId(driveUrl, "/folders/");
        List<File> files = listFilesInFolder(folderId);

        if (files.isEmpty()) {
          throw new ApiException(
                  ErrorCode.NOT_FOUND,
                  "The Google Drive folder is empty or has restricted access. Please check your google drive link."
          );
        }

        return files.stream()
                .findFirst()
                .orElseThrow(() ->
                        new ApiException(ErrorCode.NOT_FOUND,
                                "The Google Drive folder is empty."));
      }

      // Handles link if user shares direct link of file
      if (driveUrl.contains("/file/d/") || driveUrl.contains("/document/d/")) {

        String delimiter = driveUrl.contains("/file/d/") ? "/file/d/"
                : driveUrl.contains("/document/d/") ? "/document/d/"
                : null;

        if (delimiter != null) {
          return getSingleFile(extractId(driveUrl, delimiter));
        }

        throw new ApiException(
                ErrorCode.FORBIDDEN,
                "File type is invalid. Please make sure it is Txt, Pdf, Zip, or Docx!"
                );
      }

      throw new IllegalArgumentException("Invalid Google Drive URL format.");

    } catch (GoogleJsonResponseException ex) {
      // Handle specific Google API errors (e.g., 404 Not Found, 403 Forbidden)
      log.error("Google Drive API error [Status {}]: {}",
              ex.getStatusCode(),
              ex.getDetails().getMessage()
      );

      if (ex.getStatusCode() == 403) {
        throw new ApiException(
                ErrorCode.NOT_FOUND,
                "The Google Drive file or folder could not be found. Please check if the link or ID is correct."
        );
      }
      if (ex.getStatusCode() == 404) {
        throw new ApiException(
                ErrorCode.FORBIDDEN,
                "The Google Drive folder or file is set to private. " +
                        "Please change the link sharing settings to 'Anyone with the link can view'."
        );
      }

      throw new RuntimeException("Google Drive API failed: " + ex.getDetails().getMessage(), ex);

    } catch (ApiException e) {
      throw e;
    } catch (Exception ex){
      log.error("Failed to extract files from Google Drive URL: {}", driveUrl, ex);
      throw new ApiException(
              ErrorCode.INTERNAL_ERROR,
              "Failed to extract files from Google Drive URL " + ex.getMessage());
    }

  }

  /*
    Downloads the binary input stream
  */
  public InputStream downloadFileStream(String fileId){

    log.info("Retrieving binary input stream of file with ID: {}", fileId);

    if (fileId == null || fileId.isBlank()){
      log.error("FileID is null. Cannot Proceed with filestream method");
      throw new ApiException(
              ErrorCode.NOT_FOUND,
              "fileid doesnt exist!");
    }

    try {
      File metadata =
              driveService.files().get(fileId).setFields("mimeType").execute();

      String mimeType = metadata != null ? metadata.getMimeType() : "";

      if ("application/vnd.google-apps.document".equals(mimeType)) {
        log.info("Exporting native Google Doc to .docx stream for file ID: {}", fileId);
        return driveService.files()
                .export(
                  fileId,
                  "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .executeMediaAsInputStream();
      }

      log.info("Downloading binary stream for file ID: {}", fileId);
      return driveService.files().get(fileId).executeMediaAsInputStream();

    } catch (IOException ex){
      log.error("Failed to download file stream for ID: {}", fileId, ex);
      throw new RuntimeException("Failed to download file stream for ID", ex);
    }
  }

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

    List<File> files = result.getFiles().stream().toList();

    // Guard clause: enforce single file limit
    if (files.size() > 1) {
      throw new ApiException(
              ErrorCode.PAYLOAD_TOO_LARGE,
              "The provided Google Drive folder " +
              "contains multiple files. Please submit a folder with only 1 file.");
    }

    return files;
  }

  private File getSingleFile(String fileId) throws IOException {
    return driveService.files().get(fileId)
            .setFields("id, name, mimeType, webViewLink, webContentLink, iconLink, size")
            .execute();
  }
}
