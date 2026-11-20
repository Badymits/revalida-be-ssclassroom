package org.ssglobal.revalida.ssclassroom.dto.Auth;

import org.ssglobal.revalida.ssclassroom.model.CohortModel;

public record CohortOptionDto(String id, String name) {

    public static CohortOptionDto from(CohortModel cohort) {
        return new CohortOptionDto(cohort.getCohortId(), cohort.getName());
    }
}
