package org.ssglobal.revalida.ssclassroom.enums;


public enum SubmissionKind {
  FILE, TEXT, LINK;

  public String getFileType() {
    return this.name().toLowerCase();
  }
}
