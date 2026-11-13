package com.sportscenter.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RoomRequest(
        @NotBlank String name,
        String location,
        @NotNull @Positive Integer capacity,
        String status,
        @jakarta.validation.constraints.Pattern(regexp = "Indoor|Outdoor") String facilityType,
        @jakarta.validation.constraints.Size(max = 40)
        @jakarta.validation.constraints.Pattern(regexp = "C[0-9]{2,}-[A-Z0-9]+-[A-Z0-9]+") String roomCode,
        @io.swagger.v3.oas.annotations.media.Schema(implementation = RoomActivity.class,
                description = "Physical activity served; omitted/null preserves it on update. An unconfigured room cannot be newly scheduled.", nullable = true)
        String activityType) {

    public RoomRequest(String name, String location, Integer capacity, String status, String facilityType, String roomCode) {
        this(name, location, capacity, status, facilityType, roomCode, null);
    }

    public RoomRequest(String name, String location, Integer capacity, String status, String facilityType) {
        this(name, location, capacity, status, facilityType, null, null);
    }

    public RoomRequest(String name, String location, Integer capacity, String status) {
        this(name, location, capacity, status, "Indoor", null, null);
    }
}
