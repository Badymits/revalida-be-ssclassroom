package org.ssglobal.revalida.ssclassroom.dto.Assignment;

import org.ssglobal.revalida.ssclassroom.enums.AttachmentKind;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One attachment sent when a trainer saves an assignment. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentRequestDto {
    private AttachmentKind kind;
    private String name;
    private String contentType;
    private Long sizeBytes;
    private String s3Key;   // file / image
    private String url;     // link
}
