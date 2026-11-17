package org.ssglobal.revalida.ssclassroom.dto.Assignment;

import org.ssglobal.revalida.ssclassroom.enums.AttachmentKind;

import lombok.Data;

@Data
public class AttachmentUploadRequestDto {
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private AttachmentKind kind;   // file | image
}
