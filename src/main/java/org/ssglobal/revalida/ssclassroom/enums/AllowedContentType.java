package org.ssglobal.revalida.ssclassroom.enums;


import java.util.Arrays;
import java.util.Set;

public enum AllowedContentType {
  PDF(Set.of("application/pdf")),
  TXT(Set.of("text/plain")),
  ZIP(Set.of("application/zip", "application/x-zip-compressed")),
  DOCX(Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

  private final Set<String> mimeTypes;

  AllowedContentType(Set<String> mimeTypes) {
    this.mimeTypes = mimeTypes;
  }

  public static boolean contains(String contentType) {
    if (contentType == null) return false;

    String base = contentType.split(";")[0].trim().toLowerCase();

    for (AllowedContentType type : values()) {
      if (type.mimeTypes.contains(base)) {
        return true;
      }
    }
    return false;
  }
}
