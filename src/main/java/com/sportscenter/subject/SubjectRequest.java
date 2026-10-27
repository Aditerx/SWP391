package com.sportscenter.subject;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record SubjectRequest(
        @NotBlank String name,
        String description,
        @jakarta.validation.constraints.Pattern(regexp = "(?i)Active|Inactive") String status,
        String slug,
        @jakarta.validation.constraints.DecimalMin(value="0", inclusive=false) BigDecimal normalPrice,
        @jakarta.validation.constraints.DecimalMin(value="0", inclusive=false) BigDecimal advancedPrice, Boolean outdoor) {

    public SubjectRequest(String name, String description, String status, String slug, BigDecimal normalPrice, BigDecimal advancedPrice) {
        this(name, description, status, slug, normalPrice, advancedPrice, null);
    }

    public SubjectRequest(String name, String description, String status, String slug) {
        this(name, description, status, slug, null, null, null);
    }
}
