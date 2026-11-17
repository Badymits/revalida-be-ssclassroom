package org.ssglobal.revalida.ssclassroom.dto.Assignment;

import org.ssglobal.revalida.ssclassroom.enums.AttachmentKind;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponseDto {
    private String id;
    private AttachmentKind kind;
    private String name;
    private String contentType;
    private Long sizeBytes;
    private String s3Key;        // sent back so an edit can keep the file
    private String url;          // link target
    private String downloadUrl;  // temporary signed S3 URL (file / image)
}
