package org.ssglobal.revalida.ssclassroom.service.GoogleDrive;


import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;

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
      throw new IllegalArgumentException("Drive URL cannot be null or empty.");
    }

    try {
      // Handles folder read and file extraction if user shares link of folder
      // can only handle 1 file
      if (driveUrl.contains("/folders/")){
        String folderId = extractId(driveUrl, "/folders/");
        return listFilesInFolder(folderId).stream()
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException("The Google Drive folder is empty."));
      }

      // Handles link if user shares direct link of file
      if (driveUrl.contains("/file/d/")) {
        String fileId = extractId(driveUrl, "/file/d/");
        return getSingleFile(fileId);
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

  /*
    Downloads the binary input stream
  */
  public InputStream downloadFileStream(String fileId){

    log.info("Retrieving binary input stream of file with ID: {}", fileId);

    if (fileId == null || fileId.isBlank()){
      log.error("FileID is null. Cannot Proceed with filestream method");
      throw new NotFoundException("fileid doesnt exist!");
    }

    try {
      log.info("Downloading binary stream for file ID: {}", fileId);
      return driveService.files().get(fileId).executeMediaAsInputStream();
    } catch (IOException ex){
      log.error("Failed to download file stream for ID: {}", fileId);
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
      throw new IllegalArgumentException("The provided Google Drive folder contains multiple files. Please submit a folder with only 1 file.");
    }

    return files;
  }

  private File getSingleFile(String fileId) throws IOException {
    return driveService.files().get(fileId)
            .setFields("id, name, mimeType, webViewLink, webContentLink, iconLink, size")
            .execute();
  }
}
