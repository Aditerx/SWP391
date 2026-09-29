package com.sportscenter.session;

import java.util.List;

public record ConflictCheckResponse(
        boolean hasConflict,
        boolean roomConflict,
        boolean coachConflict,
        String message,
        List<String> conflictDetails
) {}
