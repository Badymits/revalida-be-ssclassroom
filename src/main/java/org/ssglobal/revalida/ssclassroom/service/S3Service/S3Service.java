// https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html
// reference to work with Amazon s3 pre-signed URLs

package org.ssglobal.revalida.ssclassroom.service.S3Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.internal.sync.FileContentStreamProvider;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;


@Service
@Slf4j
public class S3Service {

    private final S3Presigner s3Presigner;

    @Autowired
    public S3Service(S3Presigner s3Presigner){
        this.s3Presigner = s3Presigner;
    }

    //  creates time-limited URLs that grant temporary access to
    //  specific S3 objects without requiring AWS credentials, ideal for secure file sharing.
    public String createPresignedGetUrl(String bucketName, String keyName){

      log.info("Generating presigned url");
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest
                    .builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .getObjectRequest(objectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner
                    .presignGetObject(presignRequest);

            log.info("Successfully generated URL!");
            return presignedGetObjectRequest.url().toExternalForm();

        } catch (Exception e) {
          log.error("Exception: {}", e.getMessage());
          throw new RuntimeException(e);
        }
    }

    public void useSdkHttpClientToPut (
            String preSignedUrlString, File fileToPutUrl){
        log.info("Begin {} upload", fileToPutUrl.toString());

        try {
          URI presignedUrl = URI.create(preSignedUrlString);

            SdkHttpRequest requestBuilder = SdkHttpRequest.builder()
                    .method(SdkHttpMethod.PUT)
                    .uri(presignedUrl)
                    .build();

            HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                    .request(requestBuilder)
                    .contentStreamProvider(
                            new FileContentStreamProvider(
                                    fileToPutUrl.toPath())
                    )
                    .build();

            try (SdkHttpClient sdkHttpClient = ApacheHttpClient.create()){
                HttpExecuteResponse response = sdkHttpClient
                        .prepareRequest(executeRequest).call();
                log.info("Response code: {}",
                        response.httpResponse().statusCode());
            }

        } catch (IOException ex) {
          log.error("Error Occurred: {}", ex.getMessage());
        }
    }
}
