package org.ssglobal.revalida.ssclassroom.dto;

import java.time.Instant;

import org.ssglobal.revalida.ssclassroom.model.GradeDraftModel;

public record GradeDraftDto(
        Integer points,
        String privateComment,
        Instant updatedAt,
        String updatedBy) {

    public static GradeDraftDto from(GradeDraftModel draft) {
        return new GradeDraftDto(
                draft.getPoints(),
                draft.getPrivateComment(),
                draft.getUpdatedAt(),
                draft.getUpdatedBy());
    }
}