package org.ssglobal.revalida.ssclassroom.dto;

import java.time.Instant;
import java.util.List;

public record RosterResponseDto(
		Summary summary, 
		List<Item> items, 
		String nextCursor) {

    public record Summary(
    		long assigned, 
    		long submitted, 
    		long returned, 
    		long total) {
    }

    public record Item(
    		String submissionId,
    		String learnerId, 
    		String learnerLabel,
            String status, 
            Integer points,
            Instant submittedAt) {
    }
}