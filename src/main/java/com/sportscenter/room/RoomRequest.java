package com.sportscenter.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RoomRequest(
        @NotBlank String name,
        String location,
        @NotNull @Positive Integer capacity,
        String status) {
}
