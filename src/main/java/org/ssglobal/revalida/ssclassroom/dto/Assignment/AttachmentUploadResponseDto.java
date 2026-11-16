package org.ssglobal.revalida.ssclassroom.dto.Assignment;

import org.ssglobal.revalida.ssclassroom.dto.PresignedS3UrlDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUploadResponseDto {
    private String s3Key;
    /** The browser must send exactly this Content-Type with the PUT. */
    private String contentType;
    private long maxBytes;
    private PresignedS3UrlDto signedUrl;
}
