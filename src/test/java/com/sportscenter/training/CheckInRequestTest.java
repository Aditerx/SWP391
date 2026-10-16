package com.sportscenter.training;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckInRequestTest {
    @Test
    void recordedByFromLegacyRequestBodyIsIgnored() throws Exception {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        CheckInRequest request = mapper.readValue("{\"memberId\":7,\"recordedBy\":999}", CheckInRequest.class);

        assertEquals(7, request.memberId());
        assertFalse(java.util.Arrays.stream(CheckInRequest.class.getRecordComponents())
                .anyMatch(component -> component.getName().equals("recordedBy")));
    }
}
