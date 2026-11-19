package com.sportscenter.room;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer id;

    @Column(name = "room_code", nullable = false, length = 40, unique = true)
    private String roomCode;

    @PrePersist
    void assignCode() {
        if (roomCode == null || roomCode.isBlank()) {
            roomCode = "C%02d-RM-%s".formatted(centerId, java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(java.util.Locale.ROOT));
        }
    }

    @Column(name = "room_name", nullable = false, length = 50)
    private String name;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "status")
    private String status;

    @Column(name = "facility_type", length = 20)
    private String facilityType = "Indoor";

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", length = 24)
    private RoomActivity activityType;

    @Column(name = "center_id", nullable = false)
    private Integer centerId = 1;

    public boolean isOutdoor() {
        return "Outdoor".equalsIgnoreCase(facilityType);
    }
}
