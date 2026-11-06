package com.sportscenter.training;

import com.sportscenter.invoice.*;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.room.Room;
import com.sportscenter.session.Session;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.user.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Committed fixtures and real login/session, controller, service and repository; local H2 only. */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:training_access;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never", "debug=false",
        "logging.level.org.springframework=WARN", "logging.level.org.hibernate=WARN"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrainingResultAccessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @MockBean PaymentExpirationJob paymentExpirationJob;
    @MockBean PackageExpirationJob packageExpirationJob;
    @MockBean AttendanceFinalizationJob attendanceFinalizationJob;
    @MockBean NotificationScheduleJob notificationScheduleJob;

    private User member, otherMember, coach, otherCoach, manager, otherManager, receptionist, admin;
    private Session sharedSession, otherSession;
    private Integer ownResult, otherResult, ownResolution, otherResolution;
    private static final LocalDateTime RECORDED = LocalDateTime.of(2026, 11, 4, 9, 30);
    private static final String PASSWORD = "Local-access-fixture-2026!";

    @BeforeAll
    void committedFixture() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            Permission invoices = permission("MANAGE_INVOICES");
            Permission classes = permission("MANAGE_CLASSES");
            Permission users = permission("MANAGE_USERS");
            Permission result = permission("RECORD_RESULT");
            Role memberRole = role("Member");
            Role coachRole = role("Coach", result);
            Role managerRole = role("CenterManager", invoices, classes);
            Role receptionRole = role("Receptionist", invoices, result);
            Role adminRole = role("Admin", users);
            member = actor("member", memberRole, 1);
            otherMember = actor("other-member", memberRole, 1);
            User otherCenterMember = actor("other-center-member", memberRole, 2);
            coach = actor("coach", coachRole, 1);
            otherCoach = actor("other-coach", coachRole, 1);
            manager = actor("manager", managerRole, 1);
            otherManager = actor("other-manager", managerRole, 2);
            receptionist = actor("receptionist", receptionRole, 1);
            admin = actor("admin", adminRole, null);
            Room room = new Room(); room.setName("Local room"); em.persist(room);
            SportsClass sportsClass = new SportsClass();
            sportsClass.setName("Local class"); sportsClass.setCoach(coach); em.persist(sportsClass);
            sharedSession = session(sportsClass, room, coach);
            otherSession = session(sportsClass, room, otherCoach);
            ownResult = result(sharedSession, member, coach, "Own progress");
            result(sharedSession, otherMember, coach, "Other private progress");
            otherResult = result(otherSession, otherMember, otherCoach, "Other coach progress");
            ownResolution = resolution(member, "LOCAL-ACCESS-1");
            otherResolution = resolution(otherCenterMember, "LOCAL-ACCESS-2");
        });
    }

    private Permission permission(String name) {
        Permission p = new Permission(); p.setName(name); em.persist(p); return p;
    }
    private Role role(String name, Permission... permissions) {
        Role r = new Role(); r.setName(name);
        r.getPermissions().addAll(java.util.List.of(permissions)); em.persist(r); return r;
    }
    private User actor(String name, Role role, Integer center) {
        User u = new User(); u.setEmail(name + "@access.test.invalid"); u.setFullName(name);
        u.setPasswordHash(passwords.encode(PASSWORD)); u.setStatus("Active");
        u.setCenterId(center); u.setRole(role); em.persist(u);
        if ("Member".equals(role.getName())) {
            Member m = new Member(); m.setUser(u); em.persist(m);
        }
        return u;
    }
    private Session session(SportsClass sportsClass, Room room, User coach) {
        Session s = new Session(); s.setSportsClass(sportsClass); s.setRoom(room); s.setCoach(coach);
        s.setSessionDate(LocalDate.of(2026, 11, 4));
        s.setStartTime(LocalTime.of(9, 0)); s.setEndTime(LocalTime.of(10, 0)); em.persist(s); return s;
    }
    private Integer result(Session session, User member, User coach, String content) {
        TrainingResult r = new TrainingResult(); r.setSession(session); r.setMember(member);
        r.setCoach(coach); r.setContent(content); r.setRecordedAt(RECORDED); em.persist(r); return r.getResultId();
    }
    private Integer resolution(User member, String code) {
        Invoice invoice = new Invoice(); invoice.setMember(member); invoice.setInvoiceCode(code);
        invoice.setAmount(new BigDecimal("900000")); invoice.setPaymentStatus("Paid");
        invoice.setPaymentMethod("VNPay"); invoice.setCreatedAt(RECORDED);
        invoice.setReconciliationRequired(true); em.persist(invoice);
        PaymentResolution r = new PaymentResolution(); r.setInvoice(invoice); r.setType("Refund");
        r.setAmount(invoice.getAmount()); r.setReason("Local fixture");
        r.setCreatedBy(manager.getId()); r.setCreatedAt(RECORDED); em.persist(r); return r.getId();
    }
    private MockHttpSession login(User actor) throws Exception {
        var response = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"" + actor.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) response.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(actor.getId()));
        return session;
    }

    @Test void memberReadsOnlyOwnCommittedResultsWithOrWithoutMemberFilter() throws Exception {
        var session = login(member);
        for (String query : new String[]{"", "?memberId=" + member.getId(),
                "?sessionId=" + sharedSession.getId(), "?coachId=" + coach.getId()}) {
            mvc.perform(get("/api/training-results" + query).session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].resultId").value(ownResult))
                    .andExpect(jsonPath("$[0].memberId").value(member.getId()))
                    .andExpect(jsonPath("$[0].content").value("Own progress"));
        }
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM training_results", Integer.class));
        assertEquals("Own progress", jdbc.queryForObject("SELECT content FROM training_results WHERE result_id=?", String.class, ownResult));
    }

    @Test void memberCannotSelectAnotherMemberOrExpandScopeViaSessionAndCoach() throws Exception {
        var session = login(member);
        mvc.perform(get("/api/training-results").session(session).param("memberId", otherMember.getId().toString()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        for (String query : new String[]{"?sessionId=" + otherSession.getId(),
                "?coachId=" + otherCoach.getId(), "?sessionId=2147483647"}) {
            mvc.perform(get("/api/training-results" + query).session(session)).andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }

    @Test void anonymousAndMemberWritesAreRejectedWithoutChangingResults() throws Exception {
        mvc.perform(get("/api/training-results")).andExpect(status().isUnauthorized());
        var session = login(member);
        String body = "{\"sessionId\":" + sharedSession.getId() + ",\"memberId\":" + member.getId()
                + ",\"coachId\":" + coach.getId() + ",\"attendanceStatus\":\"Absent\",\"content\":\"Changed\"}";
        mvc.perform(post("/api/training-results").session(session).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/training-results/bulk").session(session).contentType("application/json")
                .content("{\"sessionId\":" + sharedSession.getId() + ",\"coachId\":" + coach.getId()
                        + ",\"items\":[{\"memberId\":" + member.getId() + ",\"attendanceStatus\":\"Absent\"}]}"))
                .andExpect(status().isForbidden());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM training_results", Integer.class));
        assertEquals("Present", jdbc.queryForObject("SELECT attendance_status FROM training_results WHERE result_id=?", String.class, ownResult));
    }

    @Test void staleSessionCannotReadAfterMemberBecomesInactiveOrChangesRole() throws Exception {
        var session = login(member);
        try {
            jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?", member.getId());
            mvc.perform(get("/api/training-results").session(session)).andExpect(status().isForbidden());
            jdbc.update("UPDATE users SET status='Active',role_id=? WHERE user_id=?", coach.getRole().getId(), member.getId());
            mvc.perform(get("/api/training-results").session(session)).andExpect(status().isForbidden());
        } finally {
            jdbc.update("UPDATE users SET status='Active',role_id=? WHERE user_id=?", member.getRole().getId(), member.getId());
        }
    }

    @Test void extraManagementAuthorityDoesNotBypassMemberOwnership() throws Exception {
        int permission = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_USERS'", Integer.class);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", member.getRole().getId(), permission);
        try {
            var session = login(member);
            mvc.perform(get("/api/training-results").session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].memberId").value(member.getId()));
            mvc.perform(get("/api/training-results").session(session).param("memberId", otherMember.getId().toString()))
                    .andExpect(status().isForbidden());
        } finally {
            jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", member.getRole().getId(), permission);
        }
    }

    @Test void coachAndAuthorizedStaffKeepExistingReadAccess() throws Exception {
        var session = login(coach);
        mvc.perform(get("/api/training-results").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].coachId", everyItem(is(coach.getId()))));
        mvc.perform(get("/api/training-results").session(session).param("coachId", otherCoach.getId().toString()))
                .andExpect(status().isForbidden());
        for (User staff : new User[]{manager, admin}) {
            mvc.perform(get("/api/training-results").session(login(staff)).param("memberId", otherMember.getId().toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[*].resultId", hasItem(otherResult)));
        }
    }

    @Test void paymentResolutionReadsRequireRoleAndAuthorityAndStayInActorsCenter() throws Exception {
        mvc.perform(get("/api/payment-resolutions")).andExpect(status().isUnauthorized());
        for (User blocked : new User[]{member, coach, admin}) {
            mvc.perform(get("/api/payment-resolutions").session(login(blocked))).andExpect(status().isForbidden());
        }
        for (User staff : new User[]{receptionist, manager, otherManager}) {
            mvc.perform(get("/api/payment-resolutions").session(login(staff))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(staff == otherManager ? otherResolution : ownResolution));
        }
        var session = login(manager);
        try {
            jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?", manager.getId());
            mvc.perform(get("/api/payment-resolutions").session(session)).andExpect(status().isForbidden());
        } finally {
            jdbc.update("UPDATE users SET status='Active' WHERE user_id=?", manager.getId());
        }
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM payment_resolutions", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE payment_status='Paid'", Integer.class));
    }

    @Test void adminWithExplicitInvoiceAuthorityStillHasNoGlobalCenterBypass() throws Exception {
        int permission = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_INVOICES'", Integer.class);
        int invoiceId = jdbc.queryForObject("SELECT invoice_id FROM payment_resolutions WHERE id=?", Integer.class, ownResolution);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", admin.getRole().getId(), permission);
        try {
            var session = login(admin);
            mvc.perform(get("/api/payment-resolutions").session(session)).andExpect(status().isOk())
                    .andExpect(content().json("[]"));
            mvc.perform(post("/api/payment-resolutions").session(session).contentType("application/json")
                            .content("{\"invoiceId\":" + invoiceId + ",\"type\":\"Refund\",\"reason\":\"Local admin check\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CENTER_REQUIRED"));
            jdbc.update("UPDATE users SET center_id=1 WHERE user_id=?", admin.getId());
            mvc.perform(get("/api/payment-resolutions").session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(ownResolution));
        } finally {
            jdbc.update("UPDATE users SET center_id=NULL WHERE user_id=?", admin.getId());
            jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", admin.getRole().getId(), permission);
        }
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM payment_resolutions", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE payment_status='Paid'", Integer.class));
    }
}
