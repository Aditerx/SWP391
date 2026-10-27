package com.sportscenter.session;

import jakarta.validation.constraints.NotNull;

public record SubstituteCoachRequest(
        @NotNull(message = "newCoachId is required")
        Integer newCoachId,

        String reason,
        boolean memberAgreed,
        String requestKey,
        Integer incidentId
) {
    public SubstituteCoachRequest(Integer id,String reason){this(id,reason,false,null,null);}
}

