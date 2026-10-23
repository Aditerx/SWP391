package com.sportscenter.session;

import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.room.Room;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.subject.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionSlotValidatorTest {

    private SessionSlotValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SessionSlotValidator();
    }

    @Test
    @DisplayName("TimeSlot enum contains exactly 7 standard slots")
    void testTimeSlotEnum_SevenStandardSlots() {
        TimeSlot[] slots = TimeSlot.values();
        assertEquals(7, slots.length);

        assertEquals(LocalTime.of(6, 0), TimeSlot.SLOT_1.getStartTime());
        assertEquals(LocalTime.of(7, 30), TimeSlot.SLOT_1.getEndTime());
        assertTrue(TimeSlot.SLOT_1.isOutdoorAllowed());

        assertEquals(LocalTime.of(7, 45), TimeSlot.SLOT_2.getStartTime());
        assertEquals(LocalTime.of(9, 15), TimeSlot.SLOT_2.getEndTime());
        assertTrue(TimeSlot.SLOT_2.isOutdoorAllowed());

        assertEquals(LocalTime.of(9, 30), TimeSlot.SLOT_3.getStartTime());
        assertEquals(LocalTime.of(11, 0), TimeSlot.SLOT_3.getEndTime());
        assertFalse(TimeSlot.SLOT_3.isOutdoorAllowed(), "Ca 3 must be blocked for outdoor");

        assertEquals(LocalTime.of(13, 0), TimeSlot.SLOT_4.getStartTime());
        assertEquals(LocalTime.of(14, 30), TimeSlot.SLOT_4.getEndTime());
        assertFalse(TimeSlot.SLOT_4.isOutdoorAllowed(), "Ca 4 must be blocked for outdoor");

        assertEquals(LocalTime.of(14, 45), TimeSlot.SLOT_5.getStartTime());
        assertEquals(LocalTime.of(16, 15), TimeSlot.SLOT_5.getEndTime());
        assertTrue(TimeSlot.SLOT_5.isOutdoorAllowed());

        assertEquals(LocalTime.of(16, 30), TimeSlot.SLOT_6.getStartTime());
        assertEquals(LocalTime.of(18, 0), TimeSlot.SLOT_6.getEndTime());
        assertTrue(TimeSlot.SLOT_6.isOutdoorAllowed());

        assertEquals(LocalTime.of(18, 15), TimeSlot.SLOT_7.getStartTime());
        assertEquals(LocalTime.of(19, 45), TimeSlot.SLOT_7.getEndTime());
        assertTrue(TimeSlot.SLOT_7.isOutdoorAllowed());
    }

    @ParameterizedTest
    @CsvSource({
            "06:00, 07:30, Ca 1",
            "07:45, 09:15, Ca 2",
            "09:30, 11:00, Ca 3",
            "13:00, 14:30, Ca 4",
            "14:45, 16:15, Ca 5",
            "16:30, 18:00, Ca 6",
            "18:15, 19:45, Ca 7"
    })
    @DisplayName("All 7 standard slots are valid for indoor")
    void testValidateSlot_IndoorValid(String start, String end, String expectedName) {
        LocalTime startTime = LocalTime.parse(start);
        LocalTime endTime = LocalTime.parse(end);

        TimeSlot slot = validator.validateSlot(startTime, endTime, false);
        assertNotNull(slot);
        assertEquals(expectedName, slot.getName());
    }

    @ParameterizedTest
    @CsvSource({
            "06:00, 07:30, Ca 1",
            "07:45, 09:15, Ca 2",
            "14:45, 16:15, Ca 5",
            "16:30, 18:00, Ca 6",
            "18:15, 19:45, Ca 7"
    })
    @DisplayName("Outdoor allows Ca 1, 2, 5, 6, 7")
    void testValidateSlot_OutdoorAllowed(String start, String end, String expectedName) {
        LocalTime startTime = LocalTime.parse(start);
        LocalTime endTime = LocalTime.parse(end);

        TimeSlot slot = validator.validateSlot(startTime, endTime, true);
        assertNotNull(slot);
        assertEquals(expectedName, slot.getName());
    }

    @Test
    @DisplayName("Outdoor blocks Ca 3 (09:30 - 11:00) due to heat policy")
    void testValidateSlot_OutdoorBlocksSlot3() {
        LocalTime startTime = LocalTime.of(9, 30);
        LocalTime endTime = LocalTime.of(11, 0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateSlot(startTime, endTime, true));
        assertTrue(ex.getMessage().contains("sốc nhiệt") || ex.getMessage().contains("nắng gắt"));
        assertTrue(ex.getMessage().contains("Ca 3"));
    }

    @Test
    @DisplayName("Outdoor blocks Ca 4 (13:00 - 14:30) due to heat policy")
    void testValidateSlot_OutdoorBlocksSlot4() {
        LocalTime startTime = LocalTime.of(13, 0);
        LocalTime endTime = LocalTime.of(14, 30);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateSlot(startTime, endTime, true));
        assertTrue(ex.getMessage().contains("sốc nhiệt") || ex.getMessage().contains("nắng gắt"));
        assertTrue(ex.getMessage().contains("Ca 4"));
    }

    @ParameterizedTest
    @CsvSource({
            "07:00, 08:30", // arbitrary time
            "06:00, 07:00", // 60 mins instead of 90 mins
            "11:00, 12:30", // during lunch break
            "11:30, 13:00", // during lunch break
            "12:00, 13:30", // overlaps lunch and ca 4
            "20:00, 21:30"  // after hours
    })
    @DisplayName("Non-standard slot throws exception")
    void testValidateSlot_NonStandard_ThrowsException(String start, String end) {
        LocalTime startTime = LocalTime.parse(start);
        LocalTime endTime = LocalTime.parse(end);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateSlot(startTime, endTime, false));
        assertTrue(ex.getMessage().contains("không hợp lệ"));
    }

    @Test
    @DisplayName("End time before start time throws exception")
    void testValidateSlot_EndBeforeStart_ThrowsException() {
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(8, 0);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validateSlot(startTime, endTime, false));
        assertTrue(ex.getMessage().contains("End time must be strictly after start time"));
    }

    @Test
    @DisplayName("Null times throw exception")
    void testValidateSlot_NullTimes_ThrowsException() {
        assertThrows(BusinessException.class, () -> validator.validateSlot(null, LocalTime.of(8, 0), false));
        assertThrows(BusinessException.class, () -> validator.validateSlot(LocalTime.of(8, 0), null, false));
    }

    @Test
    @DisplayName("Outdoor detection via Room facility_type")
    void testIsOutdoor_Room() {
        Room outdoorRoom = new Room();
        outdoorRoom.setFacilityType("Outdoor");

        Room indoorRoom = new Room();
        indoorRoom.setFacilityType("Indoor");

        assertTrue(validator.isOutdoor(null, outdoorRoom));
        assertFalse(validator.isOutdoor(null, indoorRoom));
    }

    @Test
    @DisplayName("Outdoor detection via Subject slug/name")
    void testIsOutdoor_Subject() {
        Subject tennis = new Subject();
        tennis.setSlug("quan-vot-tennis");
        tennis.setOutdoor(true); tennis.setName("Quần vợt (Tennis)");

        Subject boiLoi = new Subject();
        boiLoi.setOutdoor(true); boiLoi.setSlug("boi-loi");
        boiLoi.setName("Bơi lội");

        Subject banCung = new Subject();
        banCung.setOutdoor(true); banCung.setSlug("ban-cung");
        banCung.setName("Bắn cung");

        Subject bongDa = new Subject();
        bongDa.setOutdoor(true); bongDa.setSlug("bong-da");
        bongDa.setName("Bóng đá");

        Subject cauLong = new Subject();
        cauLong.setOutdoor(false); cauLong.setSlug("cau-long");
        cauLong.setName("Cầu lông");

        Subject pickleball = new Subject();
        pickleball.setOutdoor(false); pickleball.setSlug("pickleball");
        pickleball.setName("Pickleball");

        SportsClass tennisClass = new SportsClass();
        tennisClass.setSubject(tennis);

        SportsClass cauLongClass = new SportsClass();
        cauLongClass.setSubject(cauLong);

        assertTrue(tennis.isOutdoor());
        assertTrue(boiLoi.isOutdoor());
        assertTrue(banCung.isOutdoor());
        assertTrue(bongDa.isOutdoor());

        assertFalse(cauLong.isOutdoor());
        assertFalse(pickleball.isOutdoor());

        assertTrue(validator.isOutdoor(tennisClass, null));
        assertFalse(validator.isOutdoor(cauLongClass, null));
    }

    @Test
    @DisplayName("TimeSlot getAvailableSlots filters correctly for outdoor vs indoor")
    void testGetAvailableSlots() {
        List<TimeSlot> indoorSlots = TimeSlot.getAvailableSlots(false);
        assertEquals(7, indoorSlots.size());

        List<TimeSlot> outdoorSlots = TimeSlot.getAvailableSlots(true);
        assertEquals(5, outdoorSlots.size());
        assertFalse(outdoorSlots.contains(TimeSlot.SLOT_3));
        assertFalse(outdoorSlots.contains(TimeSlot.SLOT_4));
    }
}

