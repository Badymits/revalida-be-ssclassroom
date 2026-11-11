package org.ssglobal.revalida.ssclassroom.config;


import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveRequestInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GoogleDriveConfig {

  @Value("${google.drive.api.key}")
  private String apiKey;

  @Bean
  public Drive driveService() throws Exception {
    log.info("Setting up Google Drive service...");

    return new Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            request -> {}
    ).setApplicationName("SSclassroom")
            .setGoogleClientRequestInitializer(new DriveRequestInitializer(apiKey))
            .build();
  }
}
