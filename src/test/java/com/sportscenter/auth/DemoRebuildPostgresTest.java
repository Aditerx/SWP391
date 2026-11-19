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
import java.time.DayOfWeek;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.context.TestPropertySource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in acceptance on the unmodified new demo schema; inherits all Active demo account login checks. */
@TestPropertySource(properties="scms.early-attendance.enabled=true")
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(DemoRebuildPostgresTest.DemoWindowClock.class)
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named="scms.demo.pg-url",matches="jdbc:postgresql://127\\.0\\.0\\.1:(?:55440|54391)/scms_schema_demo_[a-z0-9_]+")
class DemoRebuildPostgresTest extends DemoSchemaLoginPostgresTest {
    @org.springframework.boot.test.context.TestConfiguration
    static class DemoWindowClock {
        @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary
        Clock presentationClock() {
            return Clock.fixed(java.time.OffsetDateTime.parse(System.getProperty("scms.demo.at")).toInstant(),java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }
    @DynamicPropertySource static void validateMappings(DynamicPropertyRegistry p) {
        p.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
    @Autowired MockMvc mvc;
    @Autowired Clock clock;
    @MockBean EmailService email;

    @org.junit.jupiter.api.Order(1)
    @Test void approvedDemoSupportsNormalAndAdvancedWorkflowsAfterCommit() throws Exception {
        JsonNode plan=json.readTree(Files.readString(Path.of(System.getProperty("scms.demo.plan"))));
        JsonNode main=plan.path("mainPaths"), normal=main.path("normal"), advanced=main.path("advanced").path("session");
        assertEquals(LocalDate.now(clock).toString(),plan.path("anchorDate").asText());
        assertEquals(20,id("SELECT count(*) FROM classes"));
        int attendanceBefore=id("SELECT count(*) FROM attendances"),resultsBefore=id("SELECT count(*) FROM training_results");
        var manager=login("manager1@gmail.com");var member=login("member14@scms.demo.invalid");
        var other=login("an@gmail.com");var cashier=login("receptionist1@gmail.com");
        var coach=login("coach.swimming1@gmail.com");var normalCoach=login("coach.badminton1@gmail.com");
        int memberId=id("SELECT user_id FROM users WHERE email='member14@scms.demo.invalid'");
        int swim=id("SELECT subject_id FROM subjects WHERE slug='boi-loi'");
        int swimCoach=id("SELECT user_id FROM users WHERE email='coach.swimming1@gmail.com'");
        int badCoach=id("SELECT user_id FROM users WHERE email='coach.badminton1@gmail.com'");
        int badRoom=id("SELECT room_id FROM rooms WHERE room_code='C01-BAD-01'");
        int swimRoom=id("SELECT room_id FROM rooms WHERE room_code='C01-SWIM-01'");
        var details=Map.of("name","Local clean demo Normal","subjectId",id("SELECT subject_id FROM subjects WHERE slug='cau-long'"),
                "coachId",badCoach,"maxCapacity",1,"minCapacity",1,"tuitionFee",900000,"startDate",normal.path("startDate").asText(),
                "status","Open","classType","Normal","totalSessions",12);
        var request=Map.of("requestId",UUID.randomUUID(),"sportsClass",details,"roomId",badRoom,
                "daysOfWeek",List.of(1,3,5),"startTime","06:00","endTime","07:30");
        int classId=write(manager,"/api/classes/normal-with-sessions",request,201).path("id").asInt();
        assertEquals(classId,write(manager,"/api/classes/normal-with-sessions",request,201).path("id").asInt());
        assertEquals(12,id("SELECT count(*) FROM sessions WHERE class_id="+classId));
        assertEquals(normal.path("startDate").asText(),jdbc.queryForObject("SELECT min(session_date)::text FROM sessions WHERE class_id=?",String.class,classId));
        int invoice=write(cashier,"/api/classes/"+classId+"/enroll",Map.of("memberId",memberId,"paymentMethod","Cash"),201).path("invoiceId").asInt();
        assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoice));
        assertEquals(900000,id("SELECT amount FROM invoices WHERE invoice_id="+invoice));
        write(cashier,"/api/invoices/"+invoice+"/confirm-cash",Map.of(),409);
        write(manager,"/api/classes/"+classId+"/confirm-opening",Map.of(),200);
        int normalSession=id("SELECT session_id FROM sessions WHERE class_id="+classId+" ORDER BY session_date,start_time LIMIT 1");
        assertTrue(read(normalCoach,"/api/sessions/"+normalSession).path("attendanceAllowed").asBoolean());
        write(normalCoach,"/api/training-results",Map.of("sessionId",normalSession,"memberId",memberId,"coachId",badCoach,"attendanceStatus","Present"),200);
        assertEquals("Scheduled",jdbc.queryForObject("SELECT status FROM sessions WHERE session_id=?",String.class,normalSession));
        int registration=id("SELECT id FROM advanced_registrations WHERE request_key='DEMO-AR01'");
        var beforeRegistration=read(member,"/api/advanced-registrations").get(0);
        assertEquals(registration,beforeRegistration.path("id").asInt());assertTrue(beforeRegistration.path("invoiceId").isNull());
        write(coach,"/api/advanced-registrations/"+registration+"/decision",Map.of("accepted",true),200);
        var order=write(member,"/api/advanced-registrations/"+registration+"/order",Map.of("paymentMethod","Cash"),200);
        int subscription=order.path("subscriptionId").asInt();assertEquals(0,credit(subscription));
        assertEquals("IDEMPOTENCY_CONFLICT",write(member,"/api/advanced-registrations/"+registration+"/order",Map.of("paymentMethod","VNPay"),409).path("code").asText());
        var retry=write(member,"/api/advanced-registrations/"+registration+"/order",Map.of("paymentMethod","Cash"),200);
        assertEquals(order.path("invoiceId"),retry.path("invoiceId"));assertEquals(order.path("expiresAt"),retry.path("expiresAt"));
        write(cashier,"/api/invoices/"+order.path("invoiceId").asInt()+"/confirm-cash",Map.of(),200);
        assertEquals(12,credit(subscription));
        var booking=new HashMap<String,Object>(Map.of("subscriptionId",subscription,"coachId",swimCoach,"subjectId",swim,"roomId",swimRoom,
                "sessionDate",advanced.path("date").asText(),"startTime",advanced.path("start").asText(),"endTime",advanced.path("end").asText(),"requestKey","clean-demo-private"));
        assertEquals("AGREEMENT_REQUIRED",write(coach,"/api/private-session-proposals",booking,400).path("code").asText());
        booking.put("agreementConfirmed",true);
        int proposal=write(coach,"/api/private-session-proposals",booking,201).path("id").asInt();
        assertEquals(12,credit(subscription));
        assertEquals(proposal,write(coach,"/api/private-session-proposals",booking,201).path("id").asInt());
        write(other,"/api/private-session-proposals/"+proposal+"/respond",Map.of("accepted",true),403);
        int session=write(member,"/api/private-session-proposals/"+proposal+"/respond",Map.of("accepted",true),200).path("sessionId").asInt();
        assertEquals(session,write(member,"/api/private-session-proposals/"+proposal+"/respond",Map.of("accepted",true),200).path("sessionId").asInt());
        assertEquals(11,credit(subscription));
        var issued=read(member,"/api/attendances/checkin-code");assertEquals(300,issued.path("expiresInSeconds").asInt());
        write(coach,"/api/attendances/scan",Map.of("sessionId",session,"memberId",memberId,"code",issued.path("payload").asText()),200);
        assertEquals(11,credit(subscription));
        assertEquals("SESSION_ALREADY_ATTENDED",write(member,"/api/sessions/"+session+"/cancel-private",Map.of(),409).path("code").asText());
        var expiry=jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",java.sql.Timestamp.class,subscription);
        JsonNode sunday=main.path("sunday").path("session");
        assertEquals(DayOfWeek.SUNDAY,LocalDate.parse(sunday.path("date").asText()).getDayOfWeek());
        booking.put("sessionDate",sunday.path("date").asText());booking.put("startTime",sunday.path("start").asText());booking.put("endTime",sunday.path("end").asText());booking.put("requestKey","clean-demo-sunday");
        int sundayProposal=write(coach,"/api/private-session-proposals",booking,201).path("id").asInt();
        int sundaySession=write(member,"/api/private-session-proposals/"+sundayProposal+"/respond",Map.of("accepted",true),200).path("sessionId").asInt();
        assertEquals(10,credit(subscription));
        write(member,"/api/sessions/"+sundaySession+"/cancel-private",Map.of(),200);assertEquals(11,credit(subscription));
        assertEquals(expiry,jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",java.sql.Timestamp.class,subscription));
        assertEquals(attendanceBefore+2,id("SELECT count(*) FROM attendances"));
        assertEquals(resultsBefore+1,id("SELECT count(*) FROM training_results"));
        assertEquals(0,id("SELECT count(*) FROM system_logs WHERE audit_category IS NULL OR scope_source IS NULL"));
    }

    @Test void approvedCancellationFixtureRefundsElevenToTwelveOnceWithoutChangingExpiry()throws Exception {
        var member=login("linh@gmail.com");
        int session=id("SELECT session_id FROM private_session_proposals WHERE request_key='DEMO-CANCEL-SUB01'");
        int sub=id("SELECT subscription_id FROM sessions WHERE session_id="+session);
        var expiry=jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",java.sql.Timestamp.class,sub);
        assertEquals(11,credit(sub));assertTrue(read(member,"/api/sessions/"+session).path("cancellationAllowed").asBoolean());
        write(member,"/api/sessions/"+session+"/cancel-private",Map.of(),200);
        assertEquals(12,credit(sub));assertTrue(jdbc.queryForObject("SELECT credit_returned FROM sessions WHERE session_id=?",Boolean.class,session));
        assertEquals("SESSION_ALREADY_CANCELLED",write(member,"/api/sessions/"+session+"/cancel-private",Map.of(),400).path("code").asText());
        assertEquals(12,credit(sub));assertEquals(expiry,jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",java.sql.Timestamp.class,sub));
        assertEquals(0,id("SELECT count(*) FROM attendances WHERE session_id="+session));
    }

    private JsonNode read(MockHttpSession session,String path)throws Exception {
        return json.readTree(mvc.perform(get(path).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test void normalRecurringSundayPublishesTwelveSessionsAndPreservesPaidMemberScope()throws Exception {
        var manager=login("manager1@gmail.com");var cashier=login("receptionist1@gmail.com");
        var member=login("member20@scms.demo.invalid");
        int memberId=id("SELECT user_id FROM users WHERE email='member20@scms.demo.invalid'");
        int coachId=id("SELECT user_id FROM users WHERE email='coach.badminton2@gmail.com'");
        int roomId=id("SELECT room_id FROM rooms WHERE room_code='C01-BAD-02'");
        LocalDate first=LocalDate.now(clock).plusDays(7);
        while(first.getDayOfWeek()!=DayOfWeek.SUNDAY)first=first.plusDays(1);
        var details=Map.of("name","Local Normal Sunday acceptance","subjectId",id("SELECT subject_id FROM subjects WHERE slug='cau-long'"),
                "coachId",coachId,"maxCapacity",1,"minCapacity",1,"tuitionFee",900000,"startDate",first.toString(),
                "status","Open","classType","Normal","totalSessions",12);
        var request=Map.of("requestId",UUID.randomUUID(),"sportsClass",details,"roomId",roomId,
                "daysOfWeek",List.of(7),"startTime","06:00","endTime","07:30");
        int course=write(manager,"/api/classes/normal-with-sessions",request,201).path("id").asInt();
        assertEquals(12,id("SELECT count(*) FROM sessions s JOIN classes c ON c.class_id=s.class_id WHERE s.class_id="+course
                +" AND extract(isodow FROM s.session_date)=7 AND coalesce(s.coach_id,c.coach_id)="+coachId+" AND s.room_id="+roomId));
        int invoice=write(cashier,"/api/classes/"+course+"/enroll",Map.of("memberId",memberId,"paymentMethod","Cash"),201).path("invoiceId").asInt();
        assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoice));
        assertEquals("Registered",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",String.class,course,memberId));
        assertFalse(write(manager,"/api/classes/"+course+"/confirm-opening",Map.of(),200).path("openingConfirmedAt").isNull());
        assertEquals(12,read(member,"/api/sessions?classId="+course).size());
        assertEquals(12,id("SELECT count(*) FROM sessions WHERE class_id="+course+" AND status='Scheduled'"));
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
