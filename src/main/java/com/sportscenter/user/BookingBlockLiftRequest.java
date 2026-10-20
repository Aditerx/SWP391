package com.sportscenter.user;
import jakarta.validation.constraints.NotBlank;
public record BookingBlockLiftRequest(@NotBlank String reason) { }
