package com.sportscenter.membership;

import jakarta.validation.constraints.NotBlank;

public record PackageStatusRequest(@NotBlank String status) {
}
