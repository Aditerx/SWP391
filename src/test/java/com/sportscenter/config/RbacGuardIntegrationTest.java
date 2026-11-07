package com.sportscenter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.invoice.*;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.room.Room;
import com.sportscenter.session.Session;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.training.*;
import com.sportscenter.user.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real login/auth-me and committed local fixtures. No cloud, seed account or mocked business service. */
@SpringBootTest(properties = {"spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:rbac_guards;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false"})
@AutoConfigureMockMvc @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RbacGuardIntegrationTest {
    @org.springframework.test.context.DynamicPropertySource
    static void disposablePostgres(org.springframework.test.context.DynamicPropertyRegistry properties) {
        String url = System.getProperty("scms.rbac.pg-url", "");
        if (url.isBlank()) return;
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+"))
            throw new IllegalArgumentException("Only a disposable loopback RBAC database is allowed");
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "scms-local-only");
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    @Autowired PlatformTransactionManager transactions;
    @Autowired SessionRegistry sessions;
    @Autowired TrainingPlanService trainingPlans;
    @Autowired EvaluationService evaluations;
    @MockBean PaymentExpirationJob paymentJob;
    @MockBean PackageExpirationJob packageJob;
    @MockBean AttendanceFinalizationJob attendanceJob;
    @MockBean NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.auth.EmailService email;
    private static final String PASSWORD = "LocalGuard123!";
    private User manager, foreignManager, admin, receptionist, coach, otherCoach, foreignCoach, member, otherMember, foreignMember;
    private SportsClass ownClass, otherClass, foreignClass;
    private Room ownRoom, foreignRoom;
    private int otherPlan, otherEvaluation, ownClassPlan, ownPrivatePlan, otherPrivatePlan;
    private Role memberRole;
    private com.sportscenter.subject.Subject subject;
    private com.sportscenter.specialization.Specialization specialization;

    @BeforeAll void fixtures() {
        new TransactionTemplate(transactions).executeWithoutResult(t -> {
            if (!System.getProperty("scms.rbac.pg-url", "").isBlank())
                jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(1,'Guard center 1'),(2,'Guard center 2') ON CONFLICT(center_id) DO NOTHING");
            var subjects = em.createQuery("select s from Subject s where s.slug='cau-long'", com.sportscenter.subject.Subject.class).getResultList();
            if (subjects.isEmpty()) {
                subject = new com.sportscenter.subject.Subject(); subject.setName("Cầu lông"); subject.setSlug("cau-long");
                subject.setNormalPrice(new BigDecimal("900000")); subject.setAdvancedPrice(new BigDecimal("3000000"));
                subject.setOutdoor(false); em.persist(subject);
            } else subject = subjects.get(0);
            specialization = new com.sportscenter.specialization.Specialization();
            specialization.setName("Guard coaching qualification"); specialization.setSubject(subject); specialization.setStatus("Active"); em.persist(specialization);
            Permission users = permission("MANAGE_USERS"), classes = permission("MANAGE_CLASSES");
            Permission plans = permission("MANAGE_TRAINING_PLAN"), results = permission("RECORD_RESULT");
            Role managerRole = role("CenterManager", users, classes);
            Role adminRole = role("Admin", users, permission("MANAGE_PERMISSIONS"), permission("MANAGE_RBAC"));
            Role coachRole = role("Coach", plans, results);
            memberRole = role("Member");
            manager = user("manager", managerRole, 1); admin = user("admin", adminRole, null);
            foreignManager = user("manager-c", managerRole, 2);
            coach = user("coach-a", coachRole, 1); otherCoach = user("coach-b", coachRole, 1);
            foreignCoach = user("coach-c", coachRole, 2);
            member = user("member-a", memberRole, 1); otherMember = user("member-b", memberRole, 1);
            foreignMember = user("member-c", memberRole, 2);
            receptionist = user("receptionist", role("Receptionist", results), 1);
            Receptionist receptionProfile = new Receptionist(); receptionProfile.setUser(receptionist); em.persist(receptionProfile);
            ownRoom = room(1); foreignRoom = room(2);
            ownClass = course(coach, 1); otherClass = course(otherCoach, 1); foreignClass = course(foreignCoach, 2);
            enrollment(member, ownClass); enrollment(otherMember, otherClass); enrollment(foreignMember, foreignClass);
            // Completed session mirrors the UI symptom: read access must not depend on the attendance write window.
            Session ended = new Session(); ended.setSportsClass(ownClass); ended.setCoach(coach); ended.setRoom(ownRoom);
            ended.setSessionDate(LocalDate.of(2026, 11, 6)); ended.setStartTime(LocalTime.of(6, 0));
            ended.setEndTime(LocalTime.of(7, 30)); ended.setStatus("Completed"); ended.setAttendanceFinalized(true); em.persist(ended);
            TrainingPlan p = new TrainingPlan(); p.setCoach(otherCoach); p.setSportsClass(otherClass);
            p.setTitle("Protected plan"); p.setCreatedAt(LocalDateTime.of(2026, 11, 6, 8, 0)); em.persist(p); otherPlan = p.getPlanId();
            ownClassPlan = plan(coach, ownClass, null, "Own shared class plan");
            ownPrivatePlan = plan(coach, null, member, "Own private plan");
            otherPrivatePlan = plan(otherCoach, null, otherMember, "Other private plan");
            Evaluation e = new Evaluation(); e.setCoach(otherCoach); e.setMember(otherMember);
            e.setEvaluationDate(LocalDate.of(2026, 11, 6)); e.setComment("Protected evaluation"); em.persist(e); otherEvaluation = e.getEvaluationId();
        });
    }

    private Permission permission(String name) { Permission p = new Permission(); p.setName(name); em.persist(p); return p; }
    private int plan(User coach, SportsClass sportsClass, User member, String title) {
        TrainingPlan p = new TrainingPlan(); p.setCoach(coach); p.setSportsClass(sportsClass); p.setMember(member);
        p.setTitle(title); p.setCreatedAt(LocalDateTime.of(2026, 11, 6, 8, 0)); em.persist(p); return p.getPlanId();
    }
    private Role role(String name, Permission... permissions) {
        Role r = new Role(); r.setName(name); r.getPermissions().addAll(List.of(permissions)); em.persist(r); return r;
    }
    private User user(String name, Role role, Integer center) {
        User u = new User(); u.setFullName(name); u.setEmail(name + "@guards.test.invalid");
        u.setPasswordHash(passwords.encode(PASSWORD)); u.setRole(role); u.setCenterId(center); u.setStatus("Active"); em.persist(u);
        if ("Member".equals(role.getName())) {
            Member m = new Member(); m.setUser(u); m.setJoinDate(LocalDate.of(2026, 11, 1)); em.persist(m);
        }
        if ("Coach".equals(role.getName())) {
            Coach c = new Coach(); c.setUser(u); em.persist(c);
            em.persist(new com.sportscenter.specialization.CoachSpecialization(c, specialization));
        }
        return u;
    }
    private Room room(int center) {
        Room r = new Room(); r.setName("Guard room " + center); r.setCenterId(center);
        r.setCapacity(4); r.setStatus("Available"); em.persist(r); return r;
    }
    private SportsClass course(User coach, int center) {
        SportsClass c = new SportsClass(); c.setName("Guard class " + coach.getId()); c.setCoach(coach);
        c.setSubject(subject);
        c.setCenterId(center); c.setStatus("Closed"); c.setMaxCapacity(4); c.setTuitionFee(new BigDecimal("900000")); em.persist(c); return c;
    }
    private void enrollment(User member, SportsClass c) {
        ClassEnrollment e = new ClassEnrollment(); e.setMember(member); e.setSportsClass(c);
        e.setStatus("Registered"); e.setEnrolledAt(LocalDateTime.of(2026, 11, 1, 8, 0)); em.persist(e);
        Invoice i = new Invoice(); i.setMember(member); i.setSportsClass(c); i.setInvoiceCode("GUARD-" + member.getId());
        i.setAmount(new BigDecimal("900000")); i.setPaymentStatus("Paid"); i.setPaymentMethod("Cash");
        i.setReceptionist(receptionist); i.setPaymentDate(LocalDateTime.of(2026, 11, 1, 8, 0));
        i.setCreatedAt(LocalDateTime.of(2026, 11, 1, 8, 0)); em.persist(i);
    }
    private MockHttpSession login(User user) throws Exception {
        MockHttpSession session = (MockHttpSession)mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()));
        return session;
    }
    private int logs() { return jdbc.queryForObject("SELECT count(*) FROM system_logs", Integer.class); }

    @Test void memberAndStaffAliasesCannotMutateAdminOrWrongSubtype() throws Exception {
        var session = login(manager); int before = logs();
        mvc.perform(post("/api/staff").session(session).contentType("application/json")
                .content("{\"name\":\"Forbidden admin\",\"email\":\"alias-admin@guards.test.invalid\",\"role\":\"Admin\"}"))
                .andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email='alias-admin@guards.test.invalid'", Integer.class));
        for (String resource : List.of("members", "staff")) {
            mvc.perform(patch("/api/" + resource + "/" + admin.getId() + "/status").session(session)
                    .contentType("application/json").content("{\"status\":\"Inactive\"}")).andExpect(status().isForbidden());
            mvc.perform(put("/api/" + resource + "/" + admin.getId()).session(session).contentType("application/json")
                    .content("{\"name\":\"Changed\",\"email\":\"unused@guards.test.invalid\",\"role\":\"Coach\"}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(patch("/api/members/" + coach.getId() + "/status").session(session).contentType("application/json")
                .content("{\"status\":\"Inactive\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/staff/" + member.getId() + "/status").session(session).contentType("application/json")
                .content("{\"status\":\"Inactive\"}")).andExpect(status().isForbidden());
        assertEquals("Active", jdbc.queryForObject("SELECT status FROM users WHERE user_id=?", String.class, admin.getId()));
        assertEquals("admin", jdbc.queryForObject("SELECT full_name FROM users WHERE user_id=?", String.class, admin.getId()));
        assertEquals(before, logs());
    }

    @Test void centerScopeAppliesToBaselineListsDetailsAndWrites() throws Exception {
        var session = login(manager); int before = logs();
        mvc.perform(get("/api/users").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(foreignCoach.getId()))));
        mvc.perform(get("/api/members").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(foreignMember.getId()))));
        mvc.perform(get("/api/staff").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(foreignCoach.getId()))));
        mvc.perform(get("/api/rooms").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(ownRoom.getId())))
                .andExpect(jsonPath("$[*].id", not(hasItem(foreignRoom.getId()))));
        mvc.perform(get("/api/users/" + foreignCoach.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/members/" + foreignMember.getId()).session(session)).andExpect(status().isForbidden());
        for (String route : List.of("members/" + foreignMember.getId(), "staff/" + foreignCoach.getId(), "users/" + foreignCoach.getId()))
            mvc.perform(patch("/api/" + route + "/status").session(session).contentType("application/json")
                    .content("{\"status\":\"Inactive\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/rooms/" + foreignRoom.getId() + "/status").session(session).contentType("application/json")
                .content("{\"status\":\"Closed\"}")).andExpect(status().isForbidden());
        assertEquals("Available", jdbc.queryForObject("SELECT status FROM rooms WHERE room_id=?", String.class, foreignRoom.getId()));
        assertEquals(before, logs());
    }

    @Test void sameCenterMemberMutationSucceedsWithRealActorAudit() throws Exception {
        var session = login(manager);
        mvc.perform(patch("/api/members/" + member.getId() + "/status").session(session)
                .contentType("application/json").content("{\"status\":\"Inactive\"}")).andExpect(status().isOk());
        try {
            assertEquals("Inactive", jdbc.queryForObject("SELECT status FROM users WHERE user_id=?", String.class, member.getId()));
            assertEquals(manager.getId(), jdbc.queryForObject("SELECT user_id FROM system_logs WHERE action='CHANGE_MEMBER_STATUS' ORDER BY log_id DESC LIMIT 1", Integer.class));
        } finally { jdbc.update("UPDATE users SET status='Active' WHERE user_id=?", member.getId()); }
    }

    @Test void coachReadsBothEnrollmentRoutesForAssignedCompletedClassOnly() throws Exception {
        var session = login(coach);
        for (String route : List.of("/api/classes/" + ownClass.getId() + "/enrollments", "/api/enrollments?classId=" + ownClass.getId()))
            mvc.perform(get(route).session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].memberId").value(member.getId()));
        for (SportsClass denied : List.of(otherClass, foreignClass))
            for (String route : List.of("/api/classes/" + denied.getId() + "/enrollments", "/api/enrollments?classId=" + denied.getId()))
                mvc.perform(get(route).session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/enrollments").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].memberId").value(member.getId()));
        mvc.perform(get("/api/enrollments?classId=" + ownClass.getId() + "&memberId=" + otherMember.getId()).session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        int count = jdbc.queryForObject("SELECT count(*) FROM class_enrollments", Integer.class);
        mvc.perform(post("/api/classes/" + ownClass.getId() + "/enroll").session(session).contentType("application/json")
                .content("{\"memberId\":" + member.getId() + ",\"paymentMethod\":\"Cash\"}")).andExpect(status().isForbidden());
        assertEquals(count, jdbc.queryForObject("SELECT count(*) FROM class_enrollments", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM invoices WHERE payment_status='Paid' AND amount=900000", Integer.class));
    }

    private String planBody(int coachId, int classId, String title) throws Exception {
        return json.writeValueAsString(Map.of("coachId", coachId, "classId", classId, "title", title));
    }
    private String evaluationBody(int coachId, int memberId, String comment) throws Exception {
        return json.writeValueAsString(Map.of("coachId", coachId, "memberId", memberId, "comment", comment));
    }
    @Test void coachCannotSpoofTrainingActorOrModifyAnotherCoachsRecords() throws Exception {
        var session = login(coach); int before = logs();
        for (String body : List.of(planBody(otherCoach.getId(), otherClass.getId(), "Spoof"), planBody(coach.getId(), otherClass.getId(), "Wrong class")))
            mvc.perform(post("/api/training-plans").session(session).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/training-plans/" + otherPlan).session(session).contentType("application/json")
                .content(planBody(coach.getId(), ownClass.getId(), "Hijack"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/training-plans/" + otherPlan).session(session)).andExpect(status().isForbidden());
        for (String body : List.of(evaluationBody(otherCoach.getId(), otherMember.getId(), "Spoof"), evaluationBody(coach.getId(), otherMember.getId(), "Unassigned")))
            mvc.perform(post("/api/evaluations").session(session).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/evaluations/" + otherEvaluation).session(session).contentType("application/json")
                .content(evaluationBody(coach.getId(), member.getId(), "Hijack"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/evaluations/" + otherEvaluation).session(session)).andExpect(status().isForbidden());
        assertEquals("Protected plan", jdbc.queryForObject("SELECT title FROM training_plans WHERE plan_id=?", String.class, otherPlan));
        assertEquals("Protected evaluation", jdbc.queryForObject("SELECT comment FROM evaluations WHERE evaluation_id=?", String.class, otherEvaluation));
        assertEquals(before, logs());
    }

    @Test void assignedCoachCanWriteUpdateAndDeleteWithAuditOfActualActor() throws Exception {
        var session = login(coach);
        var planResponse = mvc.perform(post("/api/training-plans").session(session).contentType("application/json")
                .content(planBody(coach.getId(), ownClass.getId(), "Own plan"))).andExpect(status().isCreated()).andReturn();
        int planId = json.readTree(planResponse.getResponse().getContentAsString()).path("planId").asInt();
        assertTrue(planId > 0);
        mvc.perform(put("/api/training-plans/" + planId).session(session).contentType("application/json")
                .content(planBody(coach.getId(), ownClass.getId(), "Updated"))).andExpect(status().isOk());
        assertEquals("Updated", jdbc.queryForObject("SELECT title FROM training_plans WHERE plan_id=?", String.class, planId));
        mvc.perform(delete("/api/training-plans/" + planId).session(session)).andExpect(status().isNoContent());
        var evaluationResponse = mvc.perform(post("/api/evaluations").session(session).contentType("application/json")
                .content(evaluationBody(coach.getId(), member.getId(), "Own evaluation"))).andExpect(status().isCreated()).andReturn();
        int evaluationId = json.readTree(evaluationResponse.getResponse().getContentAsString()).path("evaluationId").asInt();
        assertTrue(evaluationId > 0);
        mvc.perform(put("/api/evaluations/" + evaluationId).session(session).contentType("application/json")
                .content(evaluationBody(coach.getId(), member.getId(), "Updated"))).andExpect(status().isOk());
        assertEquals("Updated", jdbc.queryForObject("SELECT comment FROM evaluations WHERE evaluation_id=?", String.class, evaluationId));
        mvc.perform(delete("/api/evaluations/" + evaluationId).session(session)).andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM evaluations WHERE evaluation_id=?", Integer.class, evaluationId));
        assertEquals(coach.getId(), jdbc.queryForObject("SELECT user_id FROM system_logs WHERE target_entity='evaluations' ORDER BY log_id DESC LIMIT 1", Integer.class));
    }

    @Test void managementAuthorityDoesNotGrantProfessionalTrainingWrites() throws Exception {
        var session = login(manager); int before = logs();
        mvc.perform(post("/api/training-plans").session(session).contentType("application/json")
                .content(planBody(coach.getId(), ownClass.getId(), "Unauthorized manager plan"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/training-plans/" + otherPlan).session(session).contentType("application/json")
                .content(planBody(otherCoach.getId(), otherClass.getId(), "Unauthorized manager update"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/training-plans/" + otherPlan).session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/evaluations").session(session).contentType("application/json")
                .content(evaluationBody(coach.getId(), member.getId(), "Unauthorized manager evaluation"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/evaluations/" + otherEvaluation).session(session).contentType("application/json")
                .content(evaluationBody(otherCoach.getId(), otherMember.getId(), "Unauthorized update"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/evaluations/" + otherEvaluation).session(session)).andExpect(status().isForbidden());
        assertEquals("Protected plan", jdbc.queryForObject("SELECT title FROM training_plans WHERE plan_id=?", String.class, otherPlan));
        assertEquals("Protected evaluation", jdbc.queryForObject("SELECT comment FROM evaluations WHERE evaluation_id=?", String.class, otherEvaluation));
        assertEquals(before, logs());
    }

    @Test void existingSessionLosesRevokedAuthorityBeforeNextProtectedOperation() throws Exception {
        var session = login(manager);
        int permission = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_USERS'", Integer.class);
        jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", manager.getRole().getId(), permission);
        try {
            mvc.perform(get("/api/users").session(session)).andExpect(status().isForbidden());
            mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorities", not(hasItem("MANAGE_USERS"))));
        } finally { jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", manager.getRole().getId(), permission); }
    }

    @Test void inactiveAccountCannotUseAnExistingSession() throws Exception {
        var session = login(manager); jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?", manager.getId());
        try {
            mvc.perform(get("/api/users").session(session)).andExpect(status().isForbidden());
            mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isForbidden());
        } finally { jdbc.update("UPDATE users SET status='Active' WHERE user_id=?", manager.getId()); }
    }

    @Test void changedRoleRevokesOldSessionInsteadOfCombiningNewRoleAndOldAuthorities() throws Exception {
        var session = login(manager); jdbc.update("UPDATE users SET role_id=? WHERE user_id=?", memberRole.getId(), manager.getId());
        try {
            mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isForbidden());
            assertTrue(session.isInvalid());
        } finally { jdbc.update("UPDATE users SET role_id=? WHERE user_id=?", manager.getRole().getId(), manager.getId()); }
    }

    @Test void expiredRegistrySessionCannotReachAuthMe() throws Exception {
        var session = login(manager); assertNotNull(sessions.getSessionInformation(session.getId()));
        sessions.getSessionInformation(session.getId()).expireNow();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        assertTrue(session.isInvalid());
    }

    @Test void extraAuthorityDoesNotExpandCoachEnrollmentScope() throws Exception {
        int permission = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_USERS'", Integer.class);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", coach.getRole().getId(), permission);
        try {
            var session = login(coach);
            mvc.perform(get("/api/classes/" + otherClass.getId() + "/enrollments").session(session)).andExpect(status().isForbidden());
            mvc.perform(get("/api/enrollments?classId=" + otherClass.getId()).session(session)).andExpect(status().isForbidden());
            mvc.perform(get("/api/enrollments").session(session)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].memberId").value(member.getId()));
        } finally { jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", coach.getRole().getId(), permission); }
    }

    @Test void existingGlobalAccountReadForAdminIsPreserved() throws Exception {
        var session = login(admin);
        mvc.perform(get("/api/users/" + foreignCoach.getId()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/rooms").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItems(ownRoom.getId(), foreignRoom.getId())));
    }

    @Test void passwordReissueExpiresThePreviousRealSession() throws Exception {
        var oldCoachSession = login(otherCoach); var managerSession = login(manager);
        mvc.perform(post("/api/users/" + otherCoach.getId() + "/reissue-password").session(managerSession)).andExpect(status().isOk());
        try {
            mvc.perform(get("/api/auth/me").session(oldCoachSession)).andExpect(status().isUnauthorized());
            assertTrue(oldCoachSession.isInvalid());
        } finally {
            jdbc.update("UPDATE users SET password_hash=?,is_first_login=false WHERE user_id=?", passwords.encode(PASSWORD), otherCoach.getId());
        }
    }

    @Test void expiredBrowserSessionDoesNotBlockPublicPaymentCallback() throws Exception {
        var session = login(manager); sessions.getSessionInformation(session.getId()).expireNow();
        mvc.perform(get("/api/payments/vnpay/ipn").session(session)).andExpect(status().isOk());
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM invoices WHERE payment_status='Paid' AND amount=900000", Integer.class));
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test void administrativePasswordUpdateAlsoRevokesOldSessionAfterCommit() throws Exception {
        var oldSession = login(otherCoach); var managerSession = login(manager);
        mvc.perform(put("/api/users/" + otherCoach.getId()).session(managerSession).contentType("application/json")
                .content(json.writeValueAsString(Map.of("fullName", "coach-b", "email", otherCoach.getEmail(), "password", "UpdatedLocal123!"))))
                .andExpect(status().isOk());
        try {
            mvc.perform(get("/api/auth/me").session(oldSession)).andExpect(status().isUnauthorized());
            assertTrue(oldSession.isInvalid());
        } finally { jdbc.update("UPDATE users SET password_hash=? WHERE user_id=?", passwords.encode(PASSWORD), otherCoach.getId()); }
    }

    @Test void newRoomUsesTheAuthenticatedCenterInsteadOfTheEntityDefault() throws Exception {
        var session = login(foreignManager);
        var response = mvc.perform(post("/api/rooms").session(session).contentType("application/json")
                .content("{\"name\":\"Foreign center room\",\"capacity\":4,\"facilityType\":\"Indoor\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.roomCode", startsWith("C02-"))).andReturn();
        int id = json.readTree(response.getResponse().getContentAsString()).path("id").asInt();
        assertEquals(2, jdbc.queryForObject("SELECT center_id FROM rooms WHERE room_id=?", Integer.class, id));
        assertEquals(foreignManager.getId(), jdbc.queryForObject("SELECT user_id FROM system_logs WHERE action='CREATE_ROOM' AND target_id=?", Integer.class, id));
        mvc.perform(patch("/api/rooms/" + id + "/status").session(login(manager)).contentType("application/json")
                .content("{\"status\":\"Closed\"}")).andExpect(status().isForbidden());
        assertEquals("Available", jdbc.queryForObject("SELECT status FROM rooms WHERE room_id=?", String.class, id));
    }

    @Test void targetScopeIsRecheckedAfterWaitingOnConcurrentAccountChange() throws Exception {
        var session = login(manager); int before = logs();
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try (var blocker = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            blocker.setAutoCommit(false);
            try (var statement = blocker.prepareStatement("SELECT user_id FROM users WHERE user_id=? FOR UPDATE")) {
                statement.setInt(1, member.getId()); statement.executeQuery().close();
            }
            var result = executor.submit(() -> mvc.perform(patch("/api/members/" + member.getId() + "/status").session(session)
                    .contentType("application/json").content("{\"status\":\"Inactive\"}")).andReturn());
            long until = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            int waiting = 0;
            while (System.nanoTime() < until) {
                String waitingQuery = System.getProperty("scms.rbac.pg-url", "").isBlank()
                        ? "SELECT count(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL"
                        : "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'";
                waiting = jdbc.queryForObject(waitingQuery, Integer.class);
                if (waiting > 0) break;
                Thread.sleep(20);
            }
            assertTrue(waiting > 0, "HTTP mutation must wait on the controlled target row lock");
            try (var statement = blocker.prepareStatement("UPDATE users SET center_id=2 WHERE user_id=?")) {
                statement.setInt(1, member.getId()); statement.executeUpdate();
            }
            blocker.commit();
            assertEquals(403, result.get(15, java.util.concurrent.TimeUnit.SECONDS).getResponse().getStatus());
            assertEquals(2, jdbc.queryForObject("SELECT center_id FROM users WHERE user_id=?", Integer.class, member.getId()));
            assertEquals("Active", jdbc.queryForObject("SELECT status FROM users WHERE user_id=?", String.class, member.getId()));
            assertEquals(before, logs());
        } finally {
            executor.shutdownNow();
            jdbc.update("UPDATE users SET center_id=1,status='Active' WHERE user_id=?", member.getId());
        }
    }

    @Test void managerCannotCreateManageOrAssignManagerAccountsThroughAnyAlias() throws Exception {
        var session = login(manager); int before = logs();
        mvc.perform(post("/api/users").session(session).contentType("application/json")
                .content(json.writeValueAsString(Map.of("fullName", "Denied manager", "email", "denied-manager@guards.test.invalid", "roleName", "CenterManager"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/staff").session(session).contentType("application/json")
                .content("{\"name\":\"Denied manager\",\"email\":\"denied-staff-manager@guards.test.invalid\",\"role\":\"Manager\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/users/" + manager.getId()).session(session).contentType("application/json")
                .content(json.writeValueAsString(Map.of("fullName", "Forbidden change", "email", manager.getEmail()))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/staff/" + manager.getId()).session(session).contentType("application/json")
                .content(json.writeValueAsString(Map.of("name", "Forbidden change", "email", manager.getEmail(), "role", "CenterManager"))))
                .andExpect(status().isForbidden());
        for (String resource : List.of("users", "staff"))
            mvc.perform(patch("/api/" + resource + "/" + manager.getId() + "/status").session(session).contentType("application/json")
                    .content("{\"status\":\"Inactive\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/users/" + manager.getId() + "/reissue-password").session(session)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/users/" + member.getId() + "/role").session(session).contentType("application/json")
                .content("{\"roleId\":" + manager.getRole().getId() + "}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/users/" + member.getId()).session(session).contentType("application/json")
                .content(json.writeValueAsString(Map.of("fullName", "Forbidden promotion", "email", member.getEmail(), "roleName", "CenterManager"))))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/users/" + manager.getId() + "/role").session(session).contentType("application/json")
                .content("{\"roleId\":" + memberRole.getId() + "}")).andExpect(status().isForbidden());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM users WHERE email LIKE 'denied-%@guards.test.invalid'", Integer.class));
        assertEquals("manager", jdbc.queryForObject("SELECT full_name FROM users WHERE user_id=?", String.class, manager.getId()));
        assertEquals("member-a", jdbc.queryForObject("SELECT full_name FROM users WHERE user_id=?", String.class, member.getId()));
        assertEquals(manager.getRole().getId(), jdbc.queryForObject("SELECT role_id FROM users WHERE user_id=?", Integer.class, manager.getId()));
        assertEquals(memberRole.getId(), jdbc.queryForObject("SELECT role_id FROM users WHERE user_id=?", Integer.class, member.getId()));
        assertEquals(before, logs());
    }

    @Test void adminCanManageManagerAndAuditTheActualAdministrator() throws Exception {
        var session = login(admin);
        var response = mvc.perform(post("/api/users").session(session).contentType("application/json")
                .content(json.writeValueAsString(Map.of("fullName", "Managed account", "email", "managed-account@guards.test.invalid", "roleName", "CenterManager", "centerId", 1))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.roleName").value("CenterManager")).andReturn();
        int id = json.readTree(response.getResponse().getContentAsString()).path("id").asInt();
        assertEquals(admin.getId(), jdbc.queryForObject("SELECT user_id FROM system_logs WHERE action='CREATE_USER' AND target_id=?", Integer.class, id));
        mvc.perform(put("/api/staff/" + id).session(session).contentType("application/json")
                .content("{\"name\":\"Managed update\",\"email\":\"managed-account@guards.test.invalid\",\"role\":\"CenterManager\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/staff/" + id + "/status").session(session).contentType("application/json")
                .content("{\"status\":\"Inactive\"}")).andExpect(status().isOk());
        assertEquals("Inactive", jdbc.queryForObject("SELECT status FROM users WHERE user_id=?", String.class, id));
        mvc.perform(patch("/api/users/" + id + "/status").session(session).contentType("application/json")
                .content("{\"status\":\"Active\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/users/" + id + "/role").session(session).contentType("application/json")
                .content("{\"roleId\":" + coach.getRole().getId() + "}")).andExpect(status().isOk());
        mvc.perform(patch("/api/users/" + id + "/role").session(session).contentType("application/json")
                .content("{\"roleId\":" + manager.getRole().getId() + "}")).andExpect(status().isOk());
        mvc.perform(post("/api/users/" + id + "/reissue-password").session(session)).andExpect(status().isOk());
        assertEquals(manager.getRole().getId(), jdbc.queryForObject("SELECT role_id FROM users WHERE user_id=?", Integer.class, id));
        assertEquals(admin.getId(), jdbc.queryForObject("SELECT user_id FROM system_logs WHERE target_id=? ORDER BY log_id DESC LIMIT 1", Integer.class, id));
    }

    @Test void nonCoachRolesCannotWriteProfessionalContentEvenWithBothWriteAuthorities() throws Exception {
        for (User actor : List.of(manager, admin, receptionist, member)) {
            List<Integer> added = new ArrayList<>();
            for (String authority : List.of("MANAGE_TRAINING_PLAN", "RECORD_RESULT", "MANAGE_CLASSES")) {
                int permission = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name=?", Integer.class, authority);
                if (jdbc.queryForObject("SELECT count(*) FROM role_permissions WHERE role_id=? AND permission_id=?", Integer.class, actor.getRole().getId(), permission) == 0) {
                    jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", actor.getRole().getId(), permission); added.add(permission);
                }
            }
            try {
                var session = login(actor); int before = logs();
                mvc.perform(post("/api/training-plans").session(session).contentType("application/json")
                        .content(planBody(coach.getId(), ownClass.getId(), "Denied delegated write"))).andExpect(status().isForbidden());
                mvc.perform(post("/api/evaluations").session(session).contentType("application/json")
                        .content(evaluationBody(coach.getId(), member.getId(), "Denied delegated write"))).andExpect(status().isForbidden());
                assertEquals(before, logs());
            } finally { for (Integer permission : added) jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", actor.getRole().getId(), permission); }
        }
    }

    @Test void memberReadsOwnPaidClassPlanWithoutLeakingAnotherClassOrPrivateMemberPlan() throws Exception {
        var session = login(member);
        mvc.perform(get("/api/training-plans").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].planId", hasItems(ownClassPlan, ownPrivatePlan)))
                .andExpect(jsonPath("$[*].planId", not(hasItem(otherPlan))))
                .andExpect(jsonPath("$[*].planId", not(hasItem(otherPrivatePlan))));
        mvc.perform(get("/api/training-plans/" + ownClassPlan).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/training-plans/" + otherPlan).session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/training-plans/" + otherPrivatePlan).session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/training-plans?memberId=" + otherMember.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/training-plans?classId=" + otherClass.getId()).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test void classPlanReadRequiresBothRegisteredEnrollmentAndValidPaidInvoice() throws Exception {
        var session = login(member);
        try {
            for (String state : List.of("Pending", "Waitlisted", "Cancelled")) {
                jdbc.update("UPDATE class_enrollments SET status=? WHERE member_id=? AND class_id=?", state, member.getId(), ownClass.getId());
                mvc.perform(get("/api/training-plans/" + ownClassPlan).session(session)).andExpect(status().isForbidden());
                mvc.perform(get("/api/training-plans").session(session)).andExpect(status().isOk())
                        .andExpect(jsonPath("$[*].planId", not(hasItem(ownClassPlan))));
            }
            jdbc.update("UPDATE class_enrollments SET status='Registered' WHERE member_id=? AND class_id=?", member.getId(), ownClass.getId());
            jdbc.update("UPDATE invoices SET reconciliation_required=true WHERE member_id=? AND class_id=?", member.getId(), ownClass.getId());
            mvc.perform(get("/api/training-plans/" + ownClassPlan).session(session)).andExpect(status().isForbidden());
            // Existing individually targeted history has its own owner rule; no class-wide leak.
            mvc.perform(get("/api/training-plans/" + ownPrivatePlan).session(session)).andExpect(status().isOk());
        } finally {
            jdbc.update("UPDATE class_enrollments SET status='Registered' WHERE member_id=? AND class_id=?", member.getId(), ownClass.getId());
            jdbc.update("UPDATE invoices SET reconciliation_required=false WHERE member_id=? AND class_id=?", member.getId(), ownClass.getId());
        }
        mvc.perform(get("/api/training-plans/" + ownClassPlan).session(session)).andExpect(status().isOk());
    }

    @Test void serviceAlsoRejectsManagerProfessionalWritesWithoutDependingOnControllerGate() throws Exception {
        var session = login(manager); int before = logs();
        var context = (org.springframework.security.core.context.SecurityContext)session.getAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try {
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> trainingPlans.createPlan(
                    new TrainingPlanRequest(coach.getId(), ownClass.getId(), null, "Denied direct call", null, null, null, null)));
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> evaluations.createEvaluation(
                    new EvaluationRequest(member.getId(), coach.getId(), null, "Denied direct call", null)));
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        assertEquals(before, logs());
    }

    @Test void coachNeedsTheSpecificTrainingAuthorityInsteadOfManageClasses() throws Exception {
        List<Integer> permissions = jdbc.queryForList("SELECT permission_id FROM role_permissions WHERE role_id=?", Integer.class, coach.getRole().getId());
        int classesPermission = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_CLASSES'", Integer.class);
        jdbc.update("DELETE FROM role_permissions WHERE role_id=?", coach.getRole().getId());
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", coach.getRole().getId(), classesPermission);
        try {
            var session = login(coach); int before = logs();
            mvc.perform(post("/api/training-plans").session(session).contentType("application/json")
                    .content(planBody(coach.getId(), ownClass.getId(), "No plan authority"))).andExpect(status().isForbidden());
            mvc.perform(post("/api/evaluations").session(session).contentType("application/json")
                    .content(evaluationBody(coach.getId(), member.getId(), "No result authority"))).andExpect(status().isForbidden());
            assertEquals(before, logs());
        } finally {
            jdbc.update("DELETE FROM role_permissions WHERE role_id=?", coach.getRole().getId());
            for (Integer permission : permissions) jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", coach.getRole().getId(), permission);
        }
    }
}
