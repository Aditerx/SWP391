package com.sportscenter.room;

import jakarta.validation.constraints.NotBlank;

public record RoomStatusRequest(@NotBlank String status) {
}
