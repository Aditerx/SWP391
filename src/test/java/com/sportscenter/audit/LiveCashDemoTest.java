package com.sportscenter.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.room.RoomRepository;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.user.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in: writes only isolated test records in a transaction rolled back at the end. */
@SpringBootTest(properties = {"spring.profiles.active=demo", "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=OFF", "spring.jpa.show-sql=false", "spring.datasource.hikari.maximum-pool-size=2", "spring.datasource.hikari.minimum-idle=0"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named="scms.live.cash", matches="true")
class LiveCashDemoTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired MembershipPackageRepository packages;
    @Autowired SubjectRepository subjects;
    @Autowired RoomRepository rooms;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager em;
    @MockBean PaymentExpirationJob worker;
    private static final String PASSWORD = "DemoCash2026";

    private User staff(String role, String label) {
        User u = new User(); u.setFullName("CASH TEST " + label);
        u.setEmail("cash-" + UUID.randomUUID() + "@example.test");
        u.setPasswordHash(encoder.encode(PASSWORD)); u.setRole(roles.findByNameIgnoreCase(role).orElseThrow());
        u.setStatus("Active"); u.setFirstLogin(false); u.setCenterId(1);
        users.saveAndFlush(u);
        if ("Coach".equals(role)) em.createNativeQuery("INSERT INTO coaches(user_id) VALUES (:id)").setParameter("id", u.getId()).executeUpdate();
        if ("Receptionist".equals(role)) em.createNativeQuery("INSERT INTO receptionists(user_id) VALUES (:id)").setParameter("id", u.getId()).executeUpdate();
        return u;
    }
    private MockHttpSession login(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
    private JsonNode postJson(String path, MockHttpSession session, Object body, int code) throws Exception {
        var request = post(path).contentType("application/json");
        if (session != null) request.session(session);
        if (body != null) request.content(json.writeValueAsString(body));
        var result = mvc.perform(request).andExpect(status().is(code)).andReturn();
        return result.getResponse().getContentAsString().isBlank() ? json.nullNode() : json.readTree(result.getResponse().getContentAsString());
    }
    private JsonNode read(String path, MockHttpSession session) throws Exception {
        return json.readTree(mvc.perform(get(path).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test @Transactional
    void registrationCashEnrollmentAttendanceFeedbackAndReadback() throws Exception {
        var manager = staff("CenterManager", "Manager");
        var coach = staff("Coach", "Coach");
        var receptionist = staff("Receptionist", "Receptionist");
        var managerSession = login(manager.getEmail());
        var coachSession = login(coach.getEmail());
        var cashSession = login(receptionist.getEmail());
        String email = "cash-member-" + UUID.randomUUID() + "@example.test";
        postJson("/api/auth/register", null, Map.of("fullName", "CASH TEST Member", "email", email, "password", PASSWORD, "phone", "0900000000"), 200);
        assertEquals("Pending", users.findByEmailIgnoreCase(email).orElseThrow().getStatus());
        postJson("/api/auth/verify-registration", null, Map.of("email", email, "otp", "000000"), 400);
        postJson("/api/auth/verify-registration", null, Map.of("email", email, "otp", "123456"), 204);
        var memberSession = login(email);
        int memberId = read("/api/auth/me", memberSession).get("memberId").asInt();
        assertEquals(memberId, read("/api/members/me", memberSession).get("id").asInt());
        MembershipPackage pkg = new MembershipPackage(); pkg.setName("CASH TEST Package");
        pkg.setPrice(new BigDecimal("300000")); pkg.setDurationDays(30); pkg.setStatus("Active");
        packages.saveAndFlush(pkg);
        var order = postJson("/api/members/me/subscriptions", memberSession, Map.of("packageId", pkg.getId(), "paymentMethod", "Cash", "amount", 1), 201);
        int invoiceId = order.get("invoiceId").asInt();
        assertEquals(300000, order.get("amount").asInt());
        assertEquals("Pending", read("/api/members/" + memberId + "/subscriptions", memberSession).get(0).get("status").asText());
        postJson("/api/invoices/" + invoiceId + "/confirm-cash", memberSession, null, 403);
        postJson("/api/invoices/" + invoiceId + "/confirm-cash", cashSession, null, 200);
        em.flush(); em.clear();
        assertEquals("Active", read("/api/members/" + memberId + "/subscriptions", memberSession).get(0).get("status").asText());
        int subjectId = subjects.findAll().get(0).getId();
        var room = rooms.findAll().stream().filter(r -> "Available".equals(r.getStatus()) && r.getCapacity() >= 2).findFirst().orElseThrow();
        String today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString();
        var classBody = Map.of("name", "CASH TEST Paid Class", "subjectId", subjectId, "coachId", coach.getId(), "maxCapacity", 2, "tuitionFee", 150000, "startDate", today, "endDate", today, "status", "Ongoing");
        int classId = postJson("/api/classes", managerSession, classBody, 201).get("id").asInt();
        int sessionId = postJson("/api/sessions", managerSession, Map.of("classId", classId, "roomId", room.getId(), "sessionDate", today, "startTime", "05:00", "endTime", "05:30", "status", "Scheduled"), 201).get("id").asInt();
        var enrollment = postJson("/api/classes/" + classId + "/enroll", memberSession, Map.of("paymentMethod", "Cash", "amount", 1), 201);
        assertEquals("Pending", enrollment.get("status").asText());
        int tuitionInvoice = enrollment.get("invoiceId").asInt();
        assertEquals(150000, read("/api/members/me/invoices/" + tuitionInvoice, memberSession).get("amount").asInt());
        // Pending enrollments are excluded from attendance and feedback.
        postJson("/api/training-results", coachSession, Map.of("sessionId", sessionId, "memberId", memberId, "coachId", coach.getId(), "attendanceStatus", "Present"), 400);
        postJson("/api/invoices/" + tuitionInvoice + "/confirm-cash", cashSession, null, 200);
        em.flush(); em.clear();
        assertEquals("Registered", read("/api/enrollments?classId=" + classId, memberSession).get(0).get("status").asText());
        postJson("/api/classes/" + classId + "/feedback", memberSession, Map.of("classId", classId, "coachId", coach.getId(), "classRating", 5, "coachRating", 4, "comment", "Cash demo"), 403);
        postJson("/api/training-results", coachSession, Map.of("sessionId", sessionId, "memberId", memberId, "coachId", coach.getId(), "attendanceStatus", "Present", "content", "Good attendance"), 200);
        em.flush(); em.clear();
        assertEquals("Present", read("/api/attendances?sessionId=" + sessionId, memberSession).get(0).get("state").asText());
        assertEquals(1, read("/api/sessions?coachId=" + coach.getId(), coachSession).size());
        assertEquals(1, read("/api/sessions?memberId=" + memberId, memberSession).size());
        assertEquals(1, read("/api/training-results?sessionId=" + sessionId, coachSession).size());
        postJson("/api/classes/" + classId + "/feedback", memberSession, Map.of("classId", classId, "coachId", coach.getId(), "classRating", 5, "coachRating", 4, "comment", "Cash demo"), 201);
        em.flush(); em.clear();
        assertEquals(5, read("/api/classes/" + classId + "/feedback/me", memberSession).get("classRating").asInt());
        assertEquals(1, read("/api/classes/" + classId + "/feedback", managerSession).get("totalFeedback").asInt());
        var anonymous = read("/api/coaches/me/feedback?classId=" + classId, coachSession);
        assertEquals(1, anonymous.get("totalFeedback").asInt());
        assertFalse(anonymous.get("feedback").get(0).has("memberId"));
        assertFalse(anonymous.get("feedback").get(0).has("memberName"));
        postJson("/api/classes/" + classId + "/feedback", memberSession, Map.of("classId", classId, "coachId", coach.getId(), "classRating", 5, "coachRating", 4), 409);
        postJson("/api/invoices/" + invoiceId + "/confirm-cash", cashSession, null, 409);
        // Free class has no invoice, and cancellation releases the place.
        int freeClass = postJson("/api/classes", managerSession, Map.of("name", "CASH TEST Free Class", "subjectId", subjectId, "coachId", coach.getId(), "maxCapacity", 2, "tuitionFee", 0, "startDate", today, "endDate", today, "status", "Open"), 201).get("id").asInt();
        var freeEnrollment = postJson("/api/classes/" + freeClass + "/enroll", memberSession, Map.of("paymentMethod", "Cash"), 201);
        assertEquals("Registered", freeEnrollment.get("status").asText());
        assertTrue(freeEnrollment.get("invoiceId").isNull());
        // Readback after logout/login uses a new session.
        mvc.perform(post("/api/auth/logout").session(memberSession)).andExpect(status().is2xxSuccessful());
        memberSession = login(email);
        assertEquals(2, read("/api/members/me/invoices", memberSession).size());
        assertEquals(2, read("/api/invoices?keyword=CASH&status=Paid&memberId=" + memberId, cashSession).size());
    }
}
