package com.sportscenter.subject;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record SubjectRequest(
        @NotBlank String name,
        String description,
        String status,
        String slug,
        BigDecimal normalPrice,
        BigDecimal advancedPrice) {

    public SubjectRequest(String name, String description, String status, String slug) {
        this(name, description, status, slug, null, null);
    }
}
