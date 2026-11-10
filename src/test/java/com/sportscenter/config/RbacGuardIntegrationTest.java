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
    @Autowired com.sportscenter.audit.AuditService audit;
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
            else jdbc.update("INSERT INTO centers(center_id) VALUES(1),(2)");
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
        Room r = new Room(); r.setActivityType(com.sportscenter.room.RoomActivity.BADMINTON); r.setName("Guard room " + center); r.setCenterId(center);
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

    @Test void onlyAdminCanReadRoleAndPermissionCatalogsIncludingRoleDetails() throws Exception {
        List<String> paths = List.of("/api/roles", "/api/roles/" + admin.getRole().getId(), "/api/permissions");
        int authority = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_PERMISSIONS'", Integer.class);
        int before = logs();
        for (String path : paths) mvc.perform(get(path)).andExpect(status().isUnauthorized());
        var adminSession = login(admin);
        // Read access is based on the Admin role, not the authority used to mutate permissions.
        jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", admin.getRole().getId(), authority);
        try {
            mvc.perform(get("/api/roles").session(adminSession)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(admin.getRole().getId())));
            mvc.perform(get("/api/roles/" + admin.getRole().getId()).session(adminSession)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(admin.getRole().getId()));
            mvc.perform(get("/api/permissions").session(adminSession)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", hasItem("MANAGE_PERMISSIONS")));
            for (User user : List.of(manager, receptionist, coach, member)) {
                jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", user.getRole().getId(), authority);
                try {
                    var session = login(user);
                    for (String path : paths) mvc.perform(get(path).session(session)).andExpect(status().isForbidden());
                } finally {
                    jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", user.getRole().getId(), authority);
                }
            }
        } finally {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", admin.getRole().getId(), authority);
        }
        assertEquals(before, logs(), "Catalog reads and rejected requests must not write audit records");
    }

    @Test void existingAdminSessionCannotReadCatalogsAfterAccountBecomesInactiveOrLocked() throws Exception {
        int before = logs();
        for (String state : List.of("Inactive", "Locked")) {
            var session = login(admin);
            jdbc.update("UPDATE users SET status=? WHERE user_id=?", state, admin.getId());
            try {
                for (String path : List.of("/api/roles", "/api/roles/" + admin.getRole().getId(), "/api/permissions"))
                    mvc.perform(get(path).session(session)).andExpect(status().isForbidden())
                            .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
            } finally {
                jdbc.update("UPDATE users SET status='Active' WHERE user_id=?", admin.getId());
            }
        }
        assertEquals(before, logs());
    }

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

    @Test void coachFeedbackStaysAnonymousEvenWithManagementAuthority() throws Exception {
        new TransactionTemplate(transactions).executeWithoutResult(t -> {
            for (var target : List.of(ownClass, otherClass)) {
                var f = new com.sportscenter.feedback.ClassFeedback(); f.setSportsClass(target);
                f.setCoach(target.getCoach()); f.setMember(target == ownClass ? member : otherMember);
                f.setClassRating((short)5); f.setCoachRating((short)4); f.setComment("Private member review"); em.persist(f);
            }
        });
        int permission = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_CLASSES'", Integer.class);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", coach.getRole().getId(), permission);
        try {
            var a = login(coach); int before = logs();
            for (var target : List.of(ownClass, otherClass))
                mvc.perform(get("/api/classes/" + target.getId() + "/feedback").session(a)).andExpect(status().isForbidden());
            mvc.perform(get("/api/coaches/me/feedback").session(a)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.feedback.length()").value(1))
                    .andExpect(jsonPath("$.feedback[0].memberId").doesNotExist())
                    .andExpect(jsonPath("$.feedback[0].memberName").doesNotExist());
            mvc.perform(get("/api/coaches/me/feedback").param("classId",otherClass.getId().toString()).session(a))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.feedback.length()").value(0));
            assertEquals(before, logs());
        } finally { jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?",coach.getRole().getId(),permission); }
    }

    @Test void sameCenterCoachAndMemberReadMatrixCannotExpandThroughIdsFiltersOrExtraAuthority() throws Exception {
        int[] ids = new int[2];
        new TransactionTemplate(transactions).executeWithoutResult(t -> {
            int index=0;
            for (var target : List.of(ownClass,otherClass)) {
                var learner=target==ownClass?member:otherMember;
                var s=new Session(); s.setSportsClass(target); s.setCoach(target.getCoach()); s.setRoom(ownRoom);
                s.setSessionDate(LocalDate.of(2026,11,7)); s.setStartTime(LocalTime.of(6,0)); s.setEndTime(LocalTime.of(7,30));
                s.setStatus("Completed"); s.setAttendanceFinalized(true); em.persist(s); ids[index++]=s.getId();
                var a=new Attendance(); a.setSession(s); a.setMember(learner); a.setState("Absent"); em.persist(a);
                var r=new TrainingResult(); r.setSession(s); r.setCoach(target.getCoach()); r.setMember(learner);
                r.setContent("Protected session content"); r.setAttendanceStatus("Absent"); r.setRecordedAt(LocalDateTime.of(2026,11,7,7,0)); em.persist(r);
            }
        });
        int permission=jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_CLASSES'",Integer.class);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)",coach.getRole().getId(),permission);
        try {
            var a=login(coach); var m=login(member); int before=logs();
            mvc.perform(get("/api/sessions/"+ids[0]).session(a)).andExpect(status().isOk());
            mvc.perform(get("/api/sessions/"+ids[1]).session(a)).andExpect(status().isForbidden());
            mvc.perform(get("/api/sessions").param("classId",otherClass.getId().toString()).session(a)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            mvc.perform(get("/api/sessions").param("coachId",otherCoach.getId().toString()).session(a)).andExpect(status().isForbidden());
            for(String route:List.of("/api/attendances","/api/training-results")) {
                mvc.perform(get(route).param("sessionId",Integer.toString(ids[0])).session(a)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
                mvc.perform(get(route).param("sessionId",Integer.toString(ids[1])).session(a)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
                mvc.perform(get(route).param("memberId",otherMember.getId().toString()).session(m)).andExpect(status().isForbidden());
                mvc.perform(get(route).param("sessionId",Integer.toString(ids[1])).session(m)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            }
            for(String route:List.of("/api/training-plans","/api/evaluations","/api/training-results"))
                mvc.perform(get(route).param("coachId",otherCoach.getId().toString()).session(a)).andExpect(status().isForbidden());
            mvc.perform(get("/api/training-plans/"+otherPlan).session(a)).andExpect(status().isForbidden());
            mvc.perform(get("/api/evaluations/"+otherEvaluation).session(a)).andExpect(status().isForbidden());
            mvc.perform(get("/api/sessions/"+ids[1]).session(m)).andExpect(status().isForbidden());
            assertEquals(before,logs());
            assertEquals("Protected session content",jdbc.queryForObject("SELECT content FROM training_results WHERE session_id=?",String.class,ids[1]));
        } finally { jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?",coach.getRole().getId(),permission); }
    }

    @Test void invalidAccountStatusNeverReactivatesOrAuditsAnAccountThroughAnyAlias() throws Exception {
        var session = login(manager);
        for (String route : List.of("members/" + member.getId(), "users/" + member.getId(), "staff/" + otherCoach.getId())) {
            int target = route.startsWith("staff/") ? otherCoach.getId() : member.getId();
            jdbc.update("UPDATE users SET status='Locked' WHERE user_id=?", target);
            try {
                int before = logs();
                for (String body : List.of("{}", "{\"status\":null}", "{\"status\":\"\"}", "{\"status\":\"Inactve\"}")) {
                    mvc.perform(patch("/api/" + route + "/status").session(session)
                                    .contentType("application/json").content(body))
                            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ACCOUNT_STATUS"));
                    assertEquals("Locked", jdbc.queryForObject("SELECT status FROM users WHERE user_id=?", String.class, target));
                    assertEquals(before, logs());
                }
                mvc.perform(patch("/api/" + route + "/status").session(session)
                                .contentType("application/json").content("{\"status\":\" inactive \"}"))
                        .andExpect(status().isOk());
                assertEquals("Inactive", jdbc.queryForObject("SELECT status FROM users WHERE user_id=?", String.class, target));
                assertEquals(before + 1, logs());
            } finally { jdbc.update("UPDATE users SET status='Active' WHERE user_id=?", target); }
        }
    }

    @Test void permissionReplacementValidatesTheWholeSetBeforeCommitAndAuditsBeforeAfter() throws Exception {
        int target = new TransactionTemplate(transactions).execute(t -> {
            Role r = new Role(); r.setName("Local validation role");
            r.getPermissions().add(em.find(Permission.class, jdbc.queryForObject(
                    "SELECT permission_id FROM permissions WHERE permission_name='MANAGE_CLASSES'", Integer.class)));
            em.persist(r); em.flush(); return r.getId();
        });
        int users = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_USERS'", Integer.class);
        var session = login(admin); int before = logs();
        var original = jdbc.queryForList("SELECT permission_id FROM role_permissions WHERE role_id=?", Integer.class, target);
        for (String ids : List.of("[" + users + ",2147483000]", "[null]", "[-1]")) {
            mvc.perform(put("/api/roles/" + target + "/permissions").session(session).contentType("application/json")
                            .content("{\"permissionIds\":" + ids + "}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PERMISSION_IDS"));
            assertEquals(original, jdbc.queryForList("SELECT permission_id FROM role_permissions WHERE role_id=?", Integer.class, target));
            assertEquals(before, logs());
        }
        mvc.perform(put("/api/roles/" + target + "/permissions").session(session).contentType("application/json")
                        .content("{\"permissionIds\":[" + users + "," + users + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissions.length()").value(1));
        assertEquals(List.of(users), jdbc.queryForList("SELECT permission_id FROM role_permissions WHERE role_id=?", Integer.class, target));
        assertEquals(admin.getId(), jdbc.queryForObject("SELECT user_id FROM system_logs WHERE action='UPDATE_ROLE_PERMISSIONS' AND target_id=? ORDER BY log_id DESC LIMIT 1", Integer.class, target));
        assertEquals("Updated permissions for role Local validation role from: [MANAGE_CLASSES] to: [MANAGE_USERS]",
                jdbc.queryForObject("SELECT detail FROM system_logs WHERE action='UPDATE_ROLE_PERMISSIONS' AND target_id=? ORDER BY log_id DESC LIMIT 1", String.class, target));
        mvc.perform(put("/api/roles/" + target + "/permissions").session(session).contentType("application/json")
                        .content("{\"permissionIds\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.permissions.length()").value(0));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions WHERE role_id=?", Integer.class, target));
    }

    @Test void auditScopeIsCapturedAtEventTimeAndFiltersAndPagesCannotWidenIt() throws Exception {
        int authority = new TransactionTemplate(transactions).execute(t -> {
            Permission p = new Permission(); p.setName("VIEW_AUDIT_LOG"); em.persist(p); em.flush(); return p.getId();
        });
        for (var u : List.of(manager, admin, coach))
            jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", u.getRole().getId(), authority);
        try {
            new TransactionTemplate(transactions).executeWithoutResult(t -> {
                audit.log(otherCoach.getId(), "PHASE1_AUDIT_TEST", "ROOM", ownRoom.getId(), "First", LocalDateTime.of(2026,11,9,0,0));
                audit.log(otherCoach.getId(), "PHASE1_AUDIT_TEST", "ROOM", ownRoom.getId(), "Last", LocalDateTime.of(2026,11,9,23,59,59,999000000));
                audit.log(otherCoach.getId(), "PHASE1_AUDIT_TEST", "ROOM", ownRoom.getId(), "Next day", LocalDateTime.of(2026,11,10,0,0));
                audit.log(otherCoach.getId(), "PHASE1_AUDIT_TEST", "ROOM", foreignRoom.getId(), "Other center", LocalDateTime.of(2026,11,9,12,0));
                audit.log(admin.getId(), "CHANGE_PASSWORD", "USER", member.getId(), "Security", LocalDateTime.of(2026,11,9,12,0));
                audit.log(otherCoach.getId(), "PHASE1_AUDIT_TEST", "ROOM", Integer.MAX_VALUE, "Unproven target", LocalDateTime.of(2026,11,9,12,0));
            });
            jdbc.update("INSERT INTO system_logs(user_id,action,target_entity,target_id,detail,timestamp) VALUES (?,?,?,?,?,?)",
                    otherCoach.getId(), "PHASE1_AUDIT_TEST", "ROOM", ownRoom.getId(), "Legacy", LocalDateTime.of(2026,11,9,12,0));
            jdbc.update("UPDATE users SET center_id=2 WHERE user_id=?", otherCoach.getId());
            var m = login(manager); var a = login(admin); var c = login(coach); int before = logs();
            String filter = "/api/audit-logs?startDate=2026-11-09&endDate=2026-11-09&action=PHASE1_AUDIT_TEST&actorId=" + otherCoach.getId();
            mvc.perform(get(filter + "&page=0&size=1").session(m)).andExpect(status().isOk())
                    .andExpect(header().string("X-Total-Count", "2")).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].detail").value("Last"))
                    .andExpect(jsonPath("$[0].centerId").value(1)).andExpect(jsonPath("$[0].scopeSource").value("TARGET_AT_EVENT"));
            mvc.perform(get(filter + "&page=1&size=1").session(m)).andExpect(status().isOk()).andExpect(jsonPath("$[0].detail").value("First"));
            mvc.perform(get(filter).session(a)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            mvc.perform(get("/api/audit-logs?action=CHANGE_PASSWORD&startDate=2026-11-09&endDate=2026-11-09").session(a))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].category").value("SYSTEM_SECURITY"));
            mvc.perform(get("/api/audit-logs?action=CHANGE_PASSWORD").session(m)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            mvc.perform(get(filter).session(c)).andExpect(status().isForbidden());
            for (String bad : List.of("?page=-1", "?page=2147483647&size=100", "?size=101", "?size=0", "?startDate=2026-11-10&endDate=2026-11-09"))
                mvc.perform(get("/api/audit-logs" + bad).session(m)).andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("INVALID_AUDIT_FILTER"));
            assertEquals(before, logs());
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE detail='Legacy' AND audit_category IS NULL AND center_id IS NULL", Integer.class));
        } finally {
            jdbc.update("UPDATE users SET center_id=1 WHERE user_id=?", otherCoach.getId());
            for (var u : List.of(manager, admin, coach))
                jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", u.getRole().getId(), authority);
        }
    }

    @Test void coachSpecificAtRiskAndAdvancedQueuesUseTheSessionActorEvenWithExtraReportAuthority() throws Exception {
        int authority = new TransactionTemplate(transactions).execute(t -> {
            var p = em.createQuery("select p from Permission p where p.name='VIEW_REPORTS'", Permission.class).getResultStream().findFirst().orElse(null);
            if (p == null) { p = new Permission(); p.setName("VIEW_REPORTS"); em.persist(p); em.flush(); }
            return p.getId();
        });
        int[] registrations = new TransactionTemplate(transactions).execute(t -> {
            int[] ids = new int[2]; int n = 0;
            for (User owner : List.of(member, otherMember)) {
                var r = new com.sportscenter.membership.AdvancedRegistration(); r.setMember(owner); r.setSubject(subject);
                r.setCoach(owner == member ? coach : otherCoach); r.setRequestKey("local-r3-" + owner.getId());
                r.setCreatedAt(LocalDateTime.of(2026,11,9,8,0)); em.persist(r); em.flush(); ids[n++] = r.getId();
            }
            return ids;
        });
        jdbc.update("UPDATE classes SET status='Ongoing' WHERE class_id IN (?,?)", ownClass.getId(), otherClass.getId());
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", coach.getRole().getId(), authority);
        try {
            var a = login(coach); var b = login(otherCoach); var m = login(member); int before = logs();
            for (var row : List.of(new Object[]{a, member, registrations[0], registrations[1]}, new Object[]{b, otherMember, registrations[1], registrations[0]})) {
                var session = (org.springframework.mock.web.MockHttpSession)row[0]; var owner = (User)row[1];
                mvc.perform(get("/api/coaches/me/at-risk-members").param("coachId", otherCoach.getId().toString()).session(session))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].memberId").value(owner.getId()));
                mvc.perform(get("/api/members/at-risk").session(session)).andExpect(status().isForbidden());
                mvc.perform(get("/api/advanced-registrations").param("coachId", otherCoach.getId().toString()).session(session))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(row[2]));
                mvc.perform(post("/api/advanced-registrations/" + row[3] + "/decision").session(session)
                                .contentType("application/json").content("{\"accepted\":false,\"reason\":\"Not assigned\"}"))
                        .andExpect(status().isForbidden());
            }
            mvc.perform(post("/api/advanced-registrations/" + registrations[1] + "/order").session(m)
                    .contentType("application/json").content("{\"paymentMethod\":\"Cash\"}")).andExpect(status().isForbidden());
            assertEquals(before, logs());
            assertEquals("Requested", jdbc.queryForObject("SELECT status FROM advanced_registrations WHERE id=?", String.class, registrations[1]));
            for (int i = 0; i < 2; i++) mvc.perform(post("/api/advanced-registrations/" + registrations[0] + "/decision").session(a)
                    .contentType("application/json").content("{\"accepted\":false,\"reason\":\"Not accepting\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Rejected"));
            assertEquals(before + 1, logs());
            assertEquals("Rejected", jdbc.queryForObject("SELECT status FROM advanced_registrations WHERE id=?", String.class, registrations[0]));
            // Shared catalog metadata remains available; it is not a protected roster.
            mvc.perform(get("/api/classes/" + otherClass.getId()).session(a)).andExpect(status().isOk());
            mvc.perform(get("/api/classes/" + otherClass.getId()).session(m)).andExpect(status().isOk());
        } finally {
            jdbc.update("UPDATE classes SET status='Closed' WHERE class_id IN (?,?)", ownClass.getId(), otherClass.getId());
            jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", coach.getRole().getId(), authority);
        }
    }

    @Test void reportsStillRequireManagerRoleAndPermissionAndRetainTheirCenterSemantics() throws Exception {
        int authority = new TransactionTemplate(transactions).execute(t -> {
            var p = em.createQuery("select p from Permission p where p.name='VIEW_REPORTS'", Permission.class).getResultStream().findFirst().orElse(null);
            if (p == null) { p = new Permission(); p.setName("VIEW_REPORTS"); em.persist(p); em.flush(); }
            return p.getId();
        });
        for (User u : List.of(manager, admin, coach, member)) jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)",u.getRole().getId(),authority);
        try {
            var m = login(manager); var a = login(admin); var c = login(coach); var me = login(member); int before = logs();
            mvc.perform(get("/api/reports/dashboard").session(m)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUsers").value(jdbc.queryForObject("SELECT count(*) FROM users WHERE center_id=1", Integer.class)));
            mvc.perform(get("/api/reports/dashboard").session(a)).andExpect(status().isOk()).andExpect(jsonPath("$.totalUsers").value(0));
            for (String path : List.of("dashboard", "members", "revenue")) for (var s : List.of(c, me))
                mvc.perform(get("/api/reports/" + path).session(s)).andExpect(status().isForbidden());
            mvc.perform(get("/api/reports/revenue?startDate=2026-11-10&endDate=2026-11-09").session(m))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REPORT_RANGE"));
            assertEquals(before, logs());
            jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?",manager.getRole().getId(),authority);
            mvc.perform(get("/api/reports/dashboard").session(login(manager))).andExpect(status().isForbidden());
        } finally { for (User u : List.of(manager, admin, coach, member)) jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?",u.getRole().getId(),authority); }
    }

    @Test void extraUserAuthorityCannotTurnCoachOrMemberIntoAStaffProfileReader() throws Exception {
        int permission = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_USERS'", Integer.class);
        for (User u : List.of(coach, member)) jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)",u.getRole().getId(),permission);
        try {
            for (User u : List.of(coach, member)) {
                var s = login(u); int before = logs();
                for (String path : List.of("/api/members", "/api/members/"+otherMember.getId(), "/api/users", "/api/users/"+otherMember.getId(), "/api/staff"))
                    mvc.perform(get(path).session(s)).andExpect(status().isForbidden());
                mvc.perform(get("/api/classes").session(s)).andExpect(status().isOk());
                mvc.perform(get("/api/rooms").session(s)).andExpect(status().isOk());
                assertEquals(before, logs());
            }
        } finally { for (User u : List.of(coach, member)) jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?",u.getRole().getId(),permission); }
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
