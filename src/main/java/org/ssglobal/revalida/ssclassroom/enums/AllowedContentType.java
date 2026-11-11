package org.ssglobal.revalida.ssclassroom.enums;


import java.util.Arrays;

public enum AllowedContentType {
  PDF("application/pdf"),
  TXT("text/plain"),
  ZIP("application/zip"),
  DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

  private final String mimeType;

  AllowedContentType(String mimeType) {
    this.mimeType = mimeType;
  }

  // Helper method
  public static boolean contains(String mimeType) {
    if (mimeType == null) return false;
    return Arrays.stream(values())
            .anyMatch(type ->
                    type.mimeType.equalsIgnoreCase(mimeType));
  }
}
