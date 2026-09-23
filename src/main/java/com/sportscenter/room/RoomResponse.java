package com.sportscenter.room;

public record RoomResponse(Integer id, String name, String location, Integer capacity, String status) {
    static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getLocation(), room.getCapacity(), room.getStatus());
    }
}
