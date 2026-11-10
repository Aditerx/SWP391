package com.sportscenter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.user.*;
import com.sportscenter.sportclass.SportsClass;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** RP-13/TK05: real login, committed fixtures, isolated H2 or disposable loopback PostgreSQL. */
@SpringBootTest(properties={"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:publication_resources;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
    "vnpay.tmn-code=LOCALTEST", "vnpay.hash-secret=local-fixture-key", "vnpay.pay-url=http://127.0.0.1:9/pay", "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false"})
@AutoConfigureMockMvc @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicationResourceHttpTest {
    @org.springframework.test.context.DynamicPropertySource
    static void postgres(org.springframework.test.context.DynamicPropertyRegistry p) {
        String url=System.getProperty("scms.publication.pg-url", ""); if(url.isBlank()) return;
        if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+")) throw new IllegalArgumentException("Only a disposable loopback publication database is allowed");
        p.add("spring.datasource.url",()->url);p.add("spring.datasource.username",()->"postgres");p.add("spring.datasource.password",()->"scms-local-only");
        p.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");p.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect");p.add("spring.jpa.hibernate.ddl-auto",()->"none");
    }
    @Autowired MockMvc mvc; @Autowired EntityManager em; @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json; @Autowired PasswordEncoder passwords; @Autowired Clock clock;
    @Autowired PlatformTransactionManager transactions;
    @Autowired com.sportscenter.sportclass.SportsClassService classes;
    @Autowired com.sportscenter.room.RoomService roomService;
    @MockBean com.sportscenter.invoice.PaymentExpirationJob paymentJob;
    @MockBean com.sportscenter.membership.PackageExpirationJob packageJob;
    @MockBean com.sportscenter.training.AttendanceFinalizationJob attendanceJob;
    @MockBean com.sportscenter.notification.NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.auth.EmailService email;
    private User manager, member, coach;
    private SportsClass ownClass;
    private com.sportscenter.subject.Subject subject;
    private static final String PASSWORD="LocalPublication123!";

    @BeforeAll void fixture() {
        new TransactionTemplate(transactions).executeWithoutResult(t->{
            if(System.getProperty("scms.publication.pg-url", "").isBlank()) {
                jdbc.update("INSERT INTO centers(center_id) VALUES(1)");
                jdbc.execute("CREATE SEQUENCE IF NOT EXISTS invoice_code_seq START WITH 1");
            }
            Permission permission=new Permission();permission.setName("MANAGE_CLASSES");em.persist(permission);
            Role managerRole=role("CenterManager"), memberRole=role("Member"), coachRole=role("Coach"), receptionistRole=role("Receptionist");
            managerRole.getPermissions().add(permission);
            manager=user("manager",managerRole);member=user("member",memberRole);coach=user("coach",coachRole);
            User receptionist=user("cashier",receptionistRole);
            Receptionist receptionProfile=new Receptionist();receptionProfile.setUser(receptionist);em.persist(receptionProfile);
            Member profile=new Member();profile.setUser(member);profile.setJoinDate(LocalDate.now(clock).minusDays(10));em.persist(profile);
            Coach cp=new Coach();cp.setUser(coach);em.persist(cp);
            subject=em.createQuery("select s from Subject s where s.slug='cau-long'",com.sportscenter.subject.Subject.class).getResultStream().findFirst().orElse(null);
            if(subject==null){subject=new com.sportscenter.subject.Subject();subject.setSlug("cau-long");subject.setName("Publication badminton");subject.setOutdoor(false);subject.setNormalPrice(new BigDecimal("900000"));subject.setAdvancedPrice(new BigDecimal("3000000"));em.persist(subject);}
            var specialization=new com.sportscenter.specialization.Specialization();specialization.setName("Publication qualification");specialization.setSubject(subject);specialization.setStatus("Active");em.persist(specialization);
            em.persist(new com.sportscenter.specialization.CoachSpecialization(cp,specialization));
            ownClass=new SportsClass();ownClass.setName("Legacy published class");ownClass.setSubject(subject);ownClass.setCoach(coach);ownClass.setMaxCapacity(4);ownClass.setStatus("Closed");ownClass.setTuitionFee(new BigDecimal("900000"));em.persist(ownClass);
            // PostgreSQL's invoice FK targets the Receptionist subtype; flush assigned-ID profiles first.
            em.flush();
            var invoice=new com.sportscenter.invoice.Invoice();invoice.setMember(member);invoice.setSportsClass(ownClass);invoice.setInvoiceCode("PUBLICATION-OLD");invoice.setAmount(new BigDecimal("900000"));invoice.setPaymentStatus("Paid");invoice.setPaymentMethod("Cash");invoice.setReceptionist(receptionist);invoice.setCreatedAt(LocalDateTime.now(clock).minusDays(1));invoice.setPaymentDate(LocalDateTime.now(clock).minusDays(1));em.persist(invoice);
        });
    }
    private Role role(String name){Role r=new Role();r.setName(name);em.persist(r);return r;}
    private User user(String name, Role role){User u=new User();u.setFullName(name);u.setEmail(name+"@publication.test.invalid");u.setPasswordHash(passwords.encode(PASSWORD));u.setRole(role);u.setCenterId(1);u.setStatus("Active");em.persist(u);return u;}
    private MockHttpSession login(User user) throws Exception {
        var result=mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",user.getEmail(),"password",PASSWORD)))).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(user.getId()));return session;
    }
    private int logs(){return jdbc.queryForObject("SELECT count(*) FROM system_logs",Integer.class);}

    @Test void normalPublicationChoosesAnAuthorizedPriceAndOrdersKeepThatPriceAfterCatalogChanges() throws Exception {
        var m = login(manager); var me = login(member);
        BigDecimal previousDefault = jdbc.queryForObject("SELECT normal_price FROM subjects WHERE subject_id=?", BigDecimal.class, subject.getId());
        var oldInvoice = jdbc.queryForList("SELECT invoice_id,amount,payment_status FROM invoices ORDER BY invoice_id");
        var oldPurchase = jdbc.queryForList("SELECT subscription_id,purchase_amount FROM member_packages ORDER BY subscription_id");
        String roomBody = json.writeValueAsString(Map.of("name", "Publication court " + UUID.randomUUID().toString().substring(0,12), "capacity", 4,
                "status", "Available", "facilityType", "Indoor", "activityType", "BADMINTON"));
        int roomId = json.readTree(mvc.perform(post("/api/rooms").session(m).contentType("application/json").content(roomBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asInt();
        LocalDate start = LocalDate.now(clock).plusDays(3);
        var details = new HashMap<String,Object>(Map.of("name", "Publication price", "subjectId", subject.getId(), "coachId", coach.getId(),
                "maxCapacity", 4, "tuitionFee", 950000, "startDate", start.toString(), "status", "Open", "classType", "Normal", "totalSessions", 12));
        String request = json.writeValueAsString(Map.of("requestId", UUID.randomUUID(), "sportsClass", details, "roomId", roomId,
                "daysOfWeek", List.of(1,3,5), "startTime", "06:00", "endTime", "07:30"));
        var created = json.readTree(mvc.perform(post("/api/classes/normal-with-sessions").session(m).contentType("application/json").content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("Open"))
                .andExpect(jsonPath("$.tuitionFee").value(950000)).andReturn().getResponse().getContentAsString());
        int classId = created.path("id").asInt(); int afterCreate = logs();
        mvc.perform(post("/api/classes/normal-with-sessions").session(m).contentType("application/json").content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(classId));
        assertEquals(afterCreate, logs());
        assertEquals(12, jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=?", Integer.class, classId));
        assertFalse(jdbc.queryForObject("SELECT pricing_draft FROM classes WHERE class_id=?", Boolean.class, classId));
        int invoiceId = json.readTree(mvc.perform(post("/api/classes/" + classId + "/enroll").session(me).contentType("application/json")
                        .content("{\"paymentMethod\":\"VNPay\",\"amount\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.amount").value(950000)).andReturn().getResponse().getContentAsString()).path("invoiceId").asInt();
        try {
            jdbc.update("UPDATE subjects SET normal_price=1000000 WHERE subject_id=?", subject.getId());
            mvc.perform(post("/api/classes/" + classId + "/enroll").session(me).contentType("application/json").content("{\"paymentMethod\":\"VNPay\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ALREADY_ENROLLED"));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=? AND member_id=?", Integer.class, classId, member.getId()));
            details.put("tuitionFee", 1100000); int beforeReject = logs();
            mvc.perform(put("/api/classes/" + classId).session(m).contentType("application/json").content(json.writeValueAsString(details)))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CLASS_REPRICING_NOT_APPROVED"));
            assertEquals(beforeReject, logs());
            assertEquals(0, new BigDecimal("950000").compareTo(jdbc.queryForObject("SELECT tuition_fee FROM classes WHERE class_id=?", BigDecimal.class, classId)));
            assertEquals(0, new BigDecimal("950000").compareTo(jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?", BigDecimal.class, invoiceId)));
            assertEquals("Pending", jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?", String.class, invoiceId));
            details.remove("tuitionFee"); details.put("status", "Closed"); details.put("startDate", start.plusMonths(2).toString());
            var draft = json.readTree(mvc.perform(post("/api/classes").session(m).contentType("application/json").content(json.writeValueAsString(details)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.tuitionFee").value(1000000)).andReturn().getResponse().getContentAsString());
            int draftId = draft.path("id").asInt();
            mvc.perform(post("/api/sessions/generate").session(m).contentType("application/json").content(json.writeValueAsString(Map.of(
                            "classId", draftId, "roomId", roomId, "startDate", start.plusMonths(2).toString(), "daysOfWeek", List.of(2,4), "startTime", "07:45", "endTime", "09:15"))))
                    .andExpect(status().isCreated());
            details.put("tuitionFee", 975000); details.put("status", "Open");
            // Recurrence can skip the requested anchor; PUT must preserve the actual generated dates.
            details.put("startDate", jdbc.queryForObject("SELECT start_date FROM classes WHERE class_id=?", LocalDate.class, draftId).toString());
            mvc.perform(put("/api/classes/" + draftId).session(m).contentType("application/json").content(json.writeValueAsString(details)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.tuitionFee").value(975000));
            assertFalse(jdbc.queryForObject("SELECT pricing_draft FROM classes WHERE class_id=?", Boolean.class, draftId));
            assertEquals(oldInvoice, jdbc.queryForList("SELECT invoice_id,amount,payment_status FROM invoices WHERE invoice_id<>? ORDER BY invoice_id", invoiceId));
            assertEquals(oldPurchase, jdbc.queryForList("SELECT subscription_id,purchase_amount FROM member_packages ORDER BY subscription_id"));
            // Existing drafts and classes cannot be inferred from their Closed label.
            details.put("tuitionFee", 1); details.put("status", "Closed");
            mvc.perform(put("/api/classes/" + ownClass.getId()).session(m).contentType("application/json").content(json.writeValueAsString(details)))
                    .andExpect(status().isConflict());
        } finally { jdbc.update("UPDATE subjects SET normal_price=? WHERE subject_id=?", previousDefault, subject.getId()); }
    }

    @Test void roomActivityIsIndependentOfDisplayLabelsAndWrongIndoorSportsCannotSchedule() throws Exception {
        var m = login(manager);
        String body = json.writeValueAsString(Map.of("name", "Tennis display label " + UUID.randomUUID().toString().substring(0,12), "capacity", 4,
                "status", "Available", "facilityType", "Indoor", "activityType", "BASKETBALL"));
        int room = json.readTree(mvc.perform(post("/api/rooms").session(m).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asInt();
        int before = logs(), sessionsBefore = jdbc.queryForObject("SELECT count(*) FROM sessions", Integer.class);
        var req = new HashMap<String,Object>(Map.of("classId", ownClass.getId(), "roomId", room, "sessionDate", LocalDate.now(clock).plusDays(10).toString(), "startTime", "06:00", "endTime", "07:30"));
        for (String path : List.of("/api/sessions", "/api/sessions/check-conflict"))
            mvc.perform(post(path).session(m).contentType("application/json").content(json.writeValueAsString(req)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ROOM_SUBJECT_MISMATCH"));
        req.put("startDate", req.get("sessionDate")); req.put("daysOfWeek", List.of(1,3,5));
        for (String path : List.of("/api/sessions/generate", "/api/sessions/preview-recurring"))
            mvc.perform(post(path).session(m).contentType("application/json").content(json.writeValueAsString(req)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ROOM_SUBJECT_MISMATCH"));
        mvc.perform(get("/api/coaches/" + coach.getId() + "/available-slots").session(m)
                        .param("date", LocalDate.now(clock).plusDays(10).toString()).param("subjectId", subject.getId().toString()).param("roomId", Integer.toString(room)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ROOM_SUBJECT_MISMATCH"));
        assertEquals(before, logs()); assertEquals(sessionsBefore, jdbc.queryForObject("SELECT count(*) FROM sessions", Integer.class));
        String change = json.writeValueAsString(Map.of("name", "Renamed basketball court " + UUID.randomUUID().toString().substring(0,12), "roomCode", "C01-BAD-TEST" + room, "capacity", 4,
                "status", "Available", "facilityType", "Indoor", "activityType", "BASKETBALL"));
        mvc.perform(put("/api/rooms/" + room).session(m).contentType("application/json").content(change))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activityType").value("BASKETBALL"));
        mvc.perform(post("/api/sessions").session(m).contentType("application/json").content(json.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ROOM_SUBJECT_MISMATCH"));
        assertEquals("BASKETBALL", jdbc.queryForObject("SELECT activity_type FROM rooms WHERE room_id=?", String.class, room));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action='UPDATE_ROOM' AND target_id=? AND detail LIKE '%activity=BASKETBALL%' AND user_id=?", Integer.class, room, manager.getId()));
    }

    @Test void changingRoomActivityAndSchedulingSerializeOnTheSameCenterLock() throws Exception {
        var m = login(manager);
        var roomDetails = new HashMap<String,Object>(Map.of("name", "Race court " + UUID.randomUUID().toString().substring(0,12), "capacity", 4,
                "status", "Available", "facilityType", "Indoor", "activityType", "BADMINTON"));
        int room = json.readTree(mvc.perform(post("/api/rooms").session(m).contentType("application/json").content(json.writeValueAsString(roomDetails)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asInt();
        int classId = json.readTree(mvc.perform(post("/api/classes").session(m).contentType("application/json").content(json.writeValueAsString(Map.of(
                        "name", "Race draft", "subjectId", subject.getId(), "coachId", coach.getId(), "maxCapacity", 4, "status", "Closed"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asInt();
        roomDetails.put("activityType", "BASKETBALL");
        String update = json.writeValueAsString(roomDetails);
        String booking = json.writeValueAsString(Map.of("classId", classId, "roomId", room, "sessionDate", LocalDate.now(clock).plusDays(180).toString(), "startTime", "06:00", "endTime", "07:30"));
        var executor = java.util.concurrent.Executors.newFixedThreadPool(3);
        var locked = new java.util.concurrent.CountDownLatch(1); var release = new java.util.concurrent.CountDownLatch(1);
        try {
            var holder = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(t -> {
                em.find(com.sportscenter.session.ScheduleMutex.class, 1, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE); locked.countDown();
                try { if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Test lock release timed out"); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
            }));
            assertTrue(locked.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var change = executor.submit(() -> mvc.perform(put("/api/rooms/" + room).session(m).contentType("application/json").content(update)).andReturn().getResponse().getStatus());
            var schedule = executor.submit(() -> mvc.perform(post("/api/sessions").session(m).contentType("application/json").content(booking)).andReturn().getResponse().getStatus());
            assertThrows(java.util.concurrent.TimeoutException.class, () -> change.get(100, java.util.concurrent.TimeUnit.MILLISECONDS));
            release.countDown(); holder.get(15, java.util.concurrent.TimeUnit.SECONDS);
            int updateStatus = change.get(20, java.util.concurrent.TimeUnit.SECONDS), scheduleStatus = schedule.get(20, java.util.concurrent.TimeUnit.SECONDS);
            String activity = jdbc.queryForObject("SELECT activity_type FROM rooms WHERE room_id=?", String.class, room);
            int scheduled = jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=?", Integer.class, classId);
            if (scheduleStatus == 201) {
                assertEquals(400, updateStatus); assertEquals("BADMINTON", activity); assertEquals(1, scheduled);
            } else {
                assertEquals(200, updateStatus); assertEquals(400, scheduleStatus); assertEquals("BASKETBALL", activity); assertEquals(0, scheduled);
            }
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test void invalidPricesAndExtraCoachAuthorityCannotWritePublicationOrResourceConfiguration() throws Exception {
        var m = login(manager);
        int permission = jdbc.queryForObject("SELECT permission_id FROM permissions WHERE permission_name='MANAGE_CLASSES'", Integer.class);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_id) VALUES (?,?)", coach.getRole().getId(), permission);
        try {
            var c = login(coach); int before = logs();
            int classCount = jdbc.queryForObject("SELECT count(*) FROM classes", Integer.class), roomCount = jdbc.queryForObject("SELECT count(*) FROM rooms", Integer.class);
            var details = new HashMap<String,Object>(Map.of("name", "Rejected publication", "subjectId", subject.getId(), "coachId", coach.getId(), "maxCapacity", 4, "status", "Closed"));
            for (var price : List.of(BigDecimal.ZERO, new BigDecimal("0.001"))) {
                details.put("tuitionFee", price);
                mvc.perform(post("/api/classes").session(m).contentType("application/json").content(json.writeValueAsString(details)))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PRICE"));
            }
            details.put("tuitionFee", 950000);
            mvc.perform(post("/api/classes").session(c).contentType("application/json").content(json.writeValueAsString(details))).andExpect(status().isForbidden());
            mvc.perform(post("/api/rooms").session(c).contentType("application/json").content(json.writeValueAsString(Map.of(
                    "name", "Rejected room", "capacity", 4, "facilityType", "Indoor", "activityType", "BADMINTON")))).andExpect(status().isForbidden());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(coach.getEmail(), "n/a",
                            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("MANAGE_CLASSES"))));
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> classes.create(
                    new com.sportscenter.sportclass.SportsClassRequest("Rejected", subject.getId(), coach.getId(), 4, new BigDecimal("950000"), null, null, "Closed")));
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> roomService.create(
                    new com.sportscenter.room.RoomRequest("Rejected", null, 4, "Available", "Indoor", null, "BADMINTON")));
            assertEquals(before, logs());
            assertEquals(classCount, jdbc.queryForObject("SELECT count(*) FROM classes", Integer.class));
            assertEquals(roomCount, jdbc.queryForObject("SELECT count(*) FROM rooms", Integer.class));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", coach.getRole().getId(), permission);
        }
    }

}
