package com.sportscenter.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in acceptance on the unmodified new demo schema; inherits all Active demo account login checks. */
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named="scms.demo.pg-url",matches="jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_schema_demo_[a-z0-9_]+")
class DemoRebuildPostgresTest extends DemoSchemaLoginPostgresTest {
    @DynamicPropertySource static void validateMappings(DynamicPropertyRegistry p) {
        p.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
    @Autowired MockMvc mvc;
    @Autowired Clock clock;
    @MockBean EmailService email;

    @Test void approvedDemoSupportsNormalAndAdvancedWorkflowsAfterCommit() throws Exception {
        assertEquals(21,jdbc.queryForObject("SELECT count(*) FROM classes",Integer.class));
        assertEquals(7,jdbc.queryForObject("SELECT count(*) FROM normal_refunds",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM normal_refund_documents",Integer.class));
        int attendanceBefore=jdbc.queryForObject("SELECT count(*) FROM attendances",Integer.class);
        int resultsBefore=jdbc.queryForObject("SELECT count(*) FROM training_results",Integer.class);
        var manager = login("manager1@gmail.com");
        var member = login("an@gmail.com");
        var second = login("member20@scms.demo.invalid");
        var cashier = login("receptionist1@gmail.com");
        var coach = login("coach.tennis1@gmail.com");
        int badminton = id("SELECT subject_id FROM subjects WHERE slug='cau-long'");
        int tennis = id("SELECT subject_id FROM subjects WHERE slug='quan-vot-tennis'");
        int badmintonCoach = id("SELECT user_id FROM users WHERE email='coach.badminton1@gmail.com'");
        int tennisCoach = id("SELECT user_id FROM users WHERE email='coach.tennis1@gmail.com'");
        int badmintonRoom = id("SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'");
        int tennisRoom = id("SELECT room_id FROM rooms WHERE room_code='C01-TEN-01'");
        int archeryRoom = id("SELECT room_id FROM rooms WHERE room_code='C01-ARC-01'");
        LocalDate start = LocalDate.now(clock).plusDays(75);
        var details = Map.of("name", "Local clean demo Normal", "subjectId", badminton, "coachId", badmintonCoach,
                "maxCapacity", 4, "tuitionFee", 950000, "startDate", start.toString(), "status", "Open", "classType", "Normal", "totalSessions", 12);
        var request = Map.of("requestId", UUID.randomUUID(), "sportsClass", details, "roomId", badmintonRoom,
                "daysOfWeek", List.of(1,3,5), "startTime", "06:00", "endTime", "07:30");
        int classId = write(manager, "/api/classes/normal-with-sessions", request, 201).path("id").asInt();
        assertEquals(classId, write(manager, "/api/classes/normal-with-sessions", request, 201).path("id").asInt());
        assertEquals(12, jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=?", Integer.class, classId));
        assertFalse(jdbc.queryForObject("SELECT pricing_draft FROM classes WHERE class_id=?", Boolean.class, classId));
        int invoice = write(cashier, "/api/classes/"+classId+"/enroll", Map.of("memberId",2,"paymentMethod", "Cash"), 201).path("invoiceId").asInt();
        assertEquals(950000, jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?", Integer.class, invoice));
        assertEquals("Paid", jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?", String.class, invoice));
        write(cashier, "/api/invoices/"+invoice+"/confirm-cash", Map.of(), 409);
        assertEquals("Registered", jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=?", String.class, classId));

        int registration = write(second, "/api/advanced-registrations", Map.of("subjectId", tennis, "primaryCoachId", tennisCoach,
                "requestKey", "clean-demo-advanced"), 200).path("id").asInt();
        write(coach, "/api/advanced-registrations/"+registration+"/decision", Map.of("accepted", true), 200);
        var order = write(second, "/api/advanced-registrations/"+registration+"/order", Map.of("paymentMethod", "Cash"), 200);
        int subscription = order.path("subscriptionId").asInt();
        assertEquals(0, credit(subscription));
        String code = order.path("invoiceCode").asText();
        assertTrue(code.matches("INV-[0-9]{4}-[0-9]{13,}"));
        assertTrue(code.length() <= 50);
        write(cashier, "/api/invoices/"+order.path("invoiceId").asInt()+"/confirm-cash", Map.of(), 200);
        write(cashier, "/api/invoices/"+order.path("invoiceId").asInt()+"/confirm-cash", Map.of(), 409);
        assertEquals(12, credit(subscription));
        var booking = new HashMap<String,Object>(Map.of("subscriptionId", subscription, "coachId", tennisCoach,
                "subjectId", tennis, "roomId", archeryRoom, "sessionDate", LocalDate.now(clock).plusDays(3).toString(),
                "startTime", "07:45", "endTime", "09:15", "requestKey", "clean-demo-private"));
        assertEquals("AGREEMENT_REQUIRED", write(coach, "/api/sessions/book-private", booking, 400).path("code").asText());
        booking.put("agreementConfirmed", true);
        assertEquals("ROOM_SUBJECT_MISMATCH", write(coach, "/api/sessions/book-private", booking, 400).path("code").asText());
        assertEquals(12, credit(subscription));
        booking.put("roomId", tennisRoom);
        int session = write(coach, "/api/sessions/book-private", booking, 201).path("id").asInt();
        assertEquals(session, write(coach, "/api/sessions/book-private", booking, 201).path("id").asInt());
        assertEquals(11, credit(subscription));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM sessions WHERE subscription_id=?", Integer.class, subscription));
        assertEquals(attendanceBefore, jdbc.queryForObject("SELECT count(*) FROM attendances", Integer.class));
        assertEquals(resultsBefore, jdbc.queryForObject("SELECT count(*) FROM training_results", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE audit_category IS NULL OR scope_source IS NULL", Integer.class));
    }

    private int id(String sql) { return jdbc.queryForObject(sql, Integer.class); }
    @Test void seededN05ProposalTransfersToN11AndKeepsTheOriginalInvoice()throws Exception {
        var member=login("an@gmail.com");int source=id("SELECT class_id FROM classes WHERE class_name LIKE '% Normal N05'");
        int target=id("SELECT class_id FROM classes WHERE class_name LIKE '% Normal N11'");
        int invoice=id("SELECT invoice_id FROM invoices WHERE member_id=2 AND class_id="+source);
        JsonNode p=json.readTree(mvc.perform(get("/api/classes/"+source+"/normal-proposals").session(member)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get(0);
        assertEquals(12,p.path("schedule").size());
        write(member,"/api/normal-proposals/"+p.path("id").asInt()+"/respond",Map.of("version",p.path("version").asLong(),"fingerprint",p.path("fingerprint").asText(),"choice","Accept"),200);
        assertEquals(source,jdbc.queryForObject("SELECT class_id FROM invoices WHERE invoice_id=?",Integer.class,invoice));
        assertEquals(target,jdbc.queryForObject("SELECT entitlement_class_id FROM invoices WHERE invoice_id=?",Integer.class,invoice));
        assertEquals(4,jdbc.queryForObject("SELECT count(*) FROM class_enrollments WHERE class_id=? AND status='Registered'",Integer.class,target));
    }
    private int credit(int subscription) { return jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?", Integer.class, subscription); }
    private MockHttpSession login(String email) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", "SWP391"))))
                .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        return session;
    }
    private JsonNode write(MockHttpSession session, String path, Object body, int expected) throws Exception {
        var result = mvc.perform(post(path).session(session).contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().is(expected)).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
}
