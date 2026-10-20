package com.sportscenter.training;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CheckinScanRequest(@NotNull Integer memberId, @NotBlank String code, Integer sessionId) { }
