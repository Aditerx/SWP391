package com.sportscenter.room;

public record RoomResponse(
        Integer id,
        String name,
        String location,
        Integer capacity,
        String status,
        String facilityType,
        Boolean outdoor) {

    public RoomResponse(Integer id, String name, String location, Integer capacity, String status) {
        this(id, name, location, capacity, status, "Indoor", false);
    }

    public static RoomResponse from(Room room) {
        if (room == null) return null;
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getLocation(),
                room.getCapacity(),
                room.getStatus(),
                room.getFacilityType() == null ? "Indoor" : room.getFacilityType(),
                room.isOutdoor());
    }
}
