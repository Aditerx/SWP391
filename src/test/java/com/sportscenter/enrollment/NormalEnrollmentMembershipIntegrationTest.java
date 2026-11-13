package com.sportscenter.enrollment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.room.Room;
import com.sportscenter.session.Session;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.subject.Subject;
import com.sportscenter.training.AttendanceFinalizationJob;
import com.sportscenter.user.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Independent Normal tuition, committed fixtures and real authentication/JPA. No cloud or gateway calls. */
@SpringBootTest(properties = {
        "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:normal_membership;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "debug=false",
        "logging.level.org.springframework=WARN", "logging.level.org.hibernate=WARN",
        "vnpay.tmn-code=LOCALTEST", "vnpay.hash-secret=local-fixture-key-not-a-merchant-secret",
        "vnpay.pay-url=http://127.0.0.1:9/local-payment", "vnpay.return-url=http://127.0.0.1:9/local-return"
})
@AutoConfigureMockMvc
@Import(NormalEnrollmentMembershipIntegrationTest.FixedTime.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NormalEnrollmentMembershipIntegrationTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-06T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired EnrollmentService enrollments;
    @MockBean PaymentExpirationJob paymentExpirationJob;
    @MockBean PackageExpirationJob packageExpirationJob;
    @MockBean AttendanceFinalizationJob attendanceFinalizationJob;
    @MockBean NotificationScheduleJob notificationScheduleJob;
    @MockBean com.sportscenter.sportclass.NormalLifecycleJob lifecycleJob;

    private Role memberRole, coachRole, receptionistRole;
    private User member, second, otherCenter, coach, cashier;
    private SportsClass sportsClass;
    private static final String PASSWORD = "Local-Normal-access-2026!";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 8, 0);
    private int fixture;

    @BeforeAll void rolesAndCenterLocks() {
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS invoice_code_seq START WITH 1");
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            memberRole = role("Member"); coachRole = role("Coach"); receptionistRole = role("Receptionist");
            Permission p = new Permission(); p.setName("MANAGE_INVOICES"); em.persist(p);
            receptionistRole.getPermissions().add(p);
            for (String name : List.of("REGISTER_MEMBER", "MANAGE_SUBSCRIPTIONS", "MANAGE_PACKAGES")) {
                Permission permission = new Permission(); permission.setName(name); em.persist(permission);
                receptionistRole.getPermissions().add(permission);
            }
            jdbc.update("INSERT INTO centers(center_id) VALUES (1),(2)");
        });
    }
    private Role role(String name) { Role r = new Role(); r.setName(name); em.persist(r); return r; }
    @BeforeEach void committedFixture() {
        fixture++;
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            member = actor("member", memberRole, 1); second = actor("second", memberRole, 1);
            otherCenter = actor("other-center", memberRole, 2);
            coach = actor("coach", coachRole, 1); cashier = actor("cashier", receptionistRole, 1);
            Subject subject = em.createQuery("select s from Subject s where s.slug='cau-long'", Subject.class).getResultStream().findFirst().orElse(null);
            if (subject == null) { subject = new Subject(); subject.setName("Local badminton"); subject.setSlug("cau-long"); subject.setOutdoor(false); subject.setNormalPrice(new BigDecimal("900000")); em.persist(subject); }
            Room room = new Room(); room.setActivityType(com.sportscenter.room.RoomActivity.BADMINTON); room.setName("Local room " + fixture); room.setStatus("Available"); room.setCapacity(15); em.persist(room);
            sportsClass = new SportsClass(); sportsClass.setName("Normal tuition " + fixture);
            sportsClass.setClassType("Normal"); sportsClass.setSubject(subject); sportsClass.setCoach(coach);
            sportsClass.setStatus("Open"); sportsClass.setMaxCapacity(15); sportsClass.setTuitionFee(new BigDecimal("900000")); em.persist(sportsClass);
            for (int i = 1; i <= 12; i++) {
                Session s = new Session(); s.setSportsClass(sportsClass); s.setRoom(room); s.setCoach(coach);
                s.setSessionDate(NOW.toLocalDate().plusDays(i+2)); s.setStartTime(LocalTime.of(7, 45)); s.setEndTime(LocalTime.of(9, 15)); em.persist(s);
            }
        });
    }
    private User actor(String prefix, Role role, int center) {
        User u = new User(); u.setEmail(prefix + fixture + "@normal.test.invalid"); u.setFullName(prefix);
        u.setPasswordHash(passwords.encode(PASSWORD)); u.setStatus("Active"); u.setCenterId(center); u.setRole(role); em.persist(u);
        if (role == memberRole) { Member profile = new Member(); profile.setUser(u); em.persist(profile); }
        return u;
    }
    private MockHttpSession login(User actor) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("email", actor.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false); assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(actor.getId()));
        return session;
    }
    private String path() { return "/api/classes/" + sportsClass.getId() + "/enroll"; }
    private JsonNode enroll(MockHttpSession session, String method) throws Exception {
        return json.readTree(mvc.perform(post(path()).session(session).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("paymentMethod", method))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private String enrollmentStatus(User actor) {
        return jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?", String.class, sportsClass.getId(), actor.getId());
    }
    private void assertNoMemberships() {
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM member_packages WHERE member_id IN (?,?)", Integer.class, member.getId(), second.getId()));
    }

    @Test void cashIsCounterOnlyAtomicAndPriceStaysFrozen() throws Exception {
        var memberSession=login(member); var staff=login(cashier);
        for(String route:List.of(path(),"/api/enrollments")) {
            mvc.perform(post(route).session(memberSession).contentType("application/json").content(json.writeValueAsString(Map.of("classId",sportsClass.getId(),"paymentMethod","Cash"))))
                .andExpect(status().isForbidden());
        }
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=?",Integer.class,sportsClass.getId()));
        JsonNode order=json.readTree(mvc.perform(post(path()).session(staff).contentType("application/json")
            .content(json.writeValueAsString(Map.of("memberId",member.getId(),"paymentMethod","Cash"))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("Registered"))
            .andExpect(jsonPath("$.paymentStatus").value("Paid")).andReturn().getResponse().getContentAsString());
        int invoice=order.path("invoiceId").asInt();
        jdbc.update("UPDATE classes SET tuition_fee=950000 WHERE class_id=?",sportsClass.getId());
        assertEquals("Registered",enrollmentStatus(member));
        assertEquals(900000,jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?",BigDecimal.class,invoice).intValue());
        mvc.perform(post("/api/invoices/"+invoice+"/confirm-cash").session(staff)).andExpect(status().isConflict());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=?",Integer.class,sportsClass.getId()));
        assertNoMemberships();
    }

    @Test void vnpayOrderWithoutMembershipStaysPendingAndRetryDoesNotCreateAnotherInvoice() throws Exception {
        var session = login(member); JsonNode order = enroll(session, "VNPay");
        assertEquals("Pending", order.get("status").asText()); assertEquals("Pending", order.get("paymentStatus").asText());
        assertTrue(order.get("paymentUrl").asText().startsWith("http://127.0.0.1:9/local-payment?"));
        mvc.perform(post(path()).session(session).contentType("application/json").content("{\"paymentMethod\":\"VNPay\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ALREADY_ENROLLED"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE class_id=?", Integer.class, sportsClass.getId()));
        assertEquals("Pending", enrollmentStatus(member)); assertNoMemberships();
    }

    @Test void waitlistInvitationDoesNotCreateCashOrRightsAndMemberMustCheckout() throws Exception {
        jdbc.update("UPDATE classes SET max_capacity=1 WHERE class_id=?", sportsClass.getId());
        enroll(login(member), "VNPay"); JsonNode waiting = enroll(login(second), "VNPay");
        assertEquals("Waitlisted", waiting.get("status").asText());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE class_id=?", Integer.class, sportsClass.getId()));
        // Separate local fixture transition simulates a released reservation; it is not cancellation acceptance.
        jdbc.update("UPDATE class_enrollments SET status='Cancelled' WHERE class_id=? AND member_id=?", sportsClass.getId(), member.getId());
        enrollments.promoteWaitlist(sportsClass.getId(), cashier.getId());
        assertEquals("Waitlisted", enrollmentStatus(second));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=? AND member_id=?",Integer.class,sportsClass.getId(),second.getId()));
        enrollments.promoteWaitlist(sportsClass.getId(), cashier.getId());
        JsonNode order=enroll(login(second),"VNPay");
        assertEquals("Pending",order.path("status").asText());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=? AND member_id=?",Integer.class,sportsClass.getId(),second.getId()));
        assertNoMemberships();
    }

    @Test void ownerCenterInactiveAndBlockedGuardsStillRejectWithoutCreatingOrders() throws Exception {
        var session = login(member);
        mvc.perform(post(path()).session(session).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("memberId", second.getId(), "paymentMethod", "Cash"))))
                .andExpect(status().isForbidden());
        mvc.perform(post(path()).session(login(otherCenter)).contentType("application/json").content("{\"paymentMethod\":\"Cash\"}"))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?", member.getId());
        // Inactive actor is now rejected before the enrollment business handler. Staff targeting an inactive Member is a distinct case.
        mvc.perform(post(path()).session(session)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
        mvc.perform(post(path()).session(login(cashier)).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("memberId", member.getId(), "paymentMethod", "Cash"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MEMBER_NOT_ACTIVE"));
        jdbc.update("UPDATE users SET status='Active' WHERE user_id=?", member.getId());
        jdbc.update("UPDATE members SET booking_blocked_until=? WHERE user_id=?", NOW.plusHours(1), member.getId());
        mvc.perform(post(path()).session(session)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BOOKING_BLOCKED"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE class_id=?", Integer.class, sportsClass.getId()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM class_enrollments WHERE class_id=?", Integer.class, sportsClass.getId()));
        assertNoMemberships();
    }

    @Test void scheduleReadinessAndConflictsStillApplyWithoutMembership() throws Exception {
        var session = login(member);
        jdbc.update("UPDATE sessions SET status='Cancelled' WHERE session_id=(SELECT MIN(session_id) FROM sessions WHERE class_id=?)", sportsClass.getId());
        mvc.perform(post(path()).session(session)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CLASS_NOT_READY"));
        jdbc.update("UPDATE sessions SET status='Scheduled' WHERE class_id=?", sportsClass.getId());
        // A different published class with the same schedule and a committed Registered enrollment.
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            SportsClass c = new SportsClass(); c.setName("Conflict"); c.setCoach(em.getReference(User.class, coach.getId())); em.persist(c);
            Session s = new Session(); s.setSportsClass(c);
            s.setRoom(em.getReference(Room.class, jdbc.queryForObject("SELECT MIN(room_id) FROM sessions WHERE class_id=?", Integer.class, sportsClass.getId())));
            s.setSessionDate(NOW.toLocalDate().plusDays(3)); s.setStartTime(LocalTime.of(7,45)); s.setEndTime(LocalTime.of(9,15)); em.persist(s);
            ClassEnrollment e = new ClassEnrollment(); e.setMember(em.getReference(User.class, member.getId())); e.setSportsClass(c); e.setEnrolledAt(NOW); e.setStatus("Registered"); em.persist(e);
        });
        mvc.perform(post(path()).session(session)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE class_id=?", Integer.class, sportsClass.getId()));
        assertNoMemberships();
    }

    @Test void concurrentLastSeatRequestsWithoutMembershipProduceOnePendingAndOneWaitlist() throws Exception {
        jdbc.update("UPDATE classes SET max_capacity=1 WHERE class_id=?", sportsClass.getId());
        var firstSession = login(member); var secondSession = login(second);
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> requests = new ArrayList<>();
            for (var session : List.of(firstSession, secondSession)) requests.add(pool.submit(() -> {
                ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS));
                return enroll(session, "VNPay").get("status").asText();
            }));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            List<String> statuses = new ArrayList<>();
            for (var request : requests) statuses.add(request.get(20, TimeUnit.SECONDS));
            Collections.sort(statuses); assertEquals(List.of("Pending", "Waitlisted"), statuses);
        } finally { start.countDown(); pool.shutdownNow(); }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE class_id=?", Integer.class, sportsClass.getId()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM class_enrollments WHERE class_id=? AND status='Pending'", Integer.class, sportsClass.getId()));
        assertNoMemberships();
    }

    @Test void normalReenrollmentDoesNotRequireExpiredLegacyPackageOrEraseItsHistoricalLink() throws Exception {
        int oldSubscription = new TransactionTemplate(transactions).execute(tx -> {
            var subscription = new com.sportscenter.membership.MemberPackage();
            subscription.setMember(em.getReference(User.class, member.getId()));
            subscription.setStatus("Expired"); subscription.setRemainingCredit(0);
            subscription.setStartDate(NOW.toLocalDate().minusDays(60)); subscription.setEndDate(NOW.toLocalDate().minusDays(30));
            em.persist(subscription);
            ClassEnrollment old = new ClassEnrollment(); old.setMember(em.getReference(User.class, member.getId()));
            old.setSportsClass(em.getReference(SportsClass.class, sportsClass.getId())); old.setStatus("Cancelled");
            old.setEnrolledAt(NOW.minusDays(31)); old.setCancelledAt(NOW.minusDays(30));
            old.setEligibilitySubscription(subscription); em.persist(old);
            return subscription.getSubscriptionId();
        });
        JsonNode order = enroll(login(member), "VNPay");
        assertEquals("Pending", order.get("status").asText());
        assertEquals(oldSubscription, jdbc.queryForObject("SELECT eligibility_subscription_id FROM class_enrollments WHERE enrollment_id=?", Integer.class, order.get("id").asInt()));
        assertEquals("Expired", jdbc.queryForObject("SELECT status FROM member_packages WHERE subscription_id=?", String.class, oldSubscription));
        assertEquals(0, jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?", Integer.class, oldSubscription));
    }

    @Test void normalInvalidPricesRejectBothEnrollmentRoutesAndWaitlistingWithoutWrites() throws Exception {
        var session = login(member);
        for (BigDecimal price : Arrays.asList(BigDecimal.ZERO, new BigDecimal("-1"), null)) {
            // H2 deliberately admits missing legacy price so the application guard is also exercised.
            // PostgreSQL currently rejects NULL at the schema boundary.
            if (price == null) jdbc.execute("ALTER TABLE classes ALTER COLUMN tuition_fee DROP NOT NULL");
            try {
                jdbc.update("UPDATE classes SET tuition_fee=? WHERE class_id=?", price, sportsClass.getId());
                for (int capacity : new int[]{15, 0}) {
                    jdbc.update("UPDATE classes SET max_capacity=? WHERE class_id=?", capacity, sportsClass.getId());
                    for (String method : List.of("Cash", "VNPay")) {
                        for (String route : List.of(path(), "/api/enrollments")) {
                            mvc.perform(post(route).session(session).contentType("application/json")
                                            .content(json.writeValueAsString(Map.of("classId", sportsClass.getId(), "paymentMethod", method))))
                                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PRICE"));
                        }
                    }
                }
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM class_enrollments WHERE class_id=?", Integer.class, sportsClass.getId()));
                assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE class_id=?", Integer.class, sportsClass.getId()));
            } finally {
                jdbc.update("UPDATE classes SET tuition_fee=900000 WHERE class_id=?", sportsClass.getId());
                if (price == null) jdbc.execute("ALTER TABLE classes ALTER COLUMN tuition_fee SET NOT NULL");
            }
        }
        assertNoMemberships();
    }

    @Test void invalidPriceDuringPromotionKeepsWaitlistAndExistingPaidReceiptUnchanged() throws Exception {
        jdbc.update("UPDATE classes SET max_capacity=1 WHERE class_id=?", sportsClass.getId());
        JsonNode firstOrder=json.readTree(mvc.perform(post(path()).session(login(cashier)).contentType("application/json")
            .content(json.writeValueAsString(Map.of("memberId",member.getId(),"paymentMethod","Cash"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        int paidInvoice=firstOrder.path("invoiceId").asInt();
        JsonNode waiting = enroll(login(second), "VNPay");
        assertEquals("Waitlisted", waiting.get("status").asText());
        jdbc.update("UPDATE classes SET max_capacity=2 WHERE class_id=?", sportsClass.getId());
        for (BigDecimal price : List.of(BigDecimal.ZERO, new BigDecimal("-1"))) {
            jdbc.update("UPDATE classes SET tuition_fee=? WHERE class_id=?", price, sportsClass.getId());
            var error = assertThrows(com.sportscenter.common.exception.BusinessException.class,
                    () -> enrollments.promoteWaitlist(sportsClass.getId(), cashier.getId()));
            assertTrue(error.getMessage().startsWith("INVALID_PRICE:"));
            assertEquals("Waitlisted", enrollmentStatus(second));
            assertNull(jdbc.queryForObject("SELECT promoted_at FROM class_enrollments WHERE enrollment_id=?", LocalDateTime.class, waiting.get("id").asInt()));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE class_id=?", Integer.class, sportsClass.getId()));
            assertEquals("Registered", enrollmentStatus(member));
            assertEquals("Paid", jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?", String.class, paidInvoice));
            assertEquals(900000, jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?", BigDecimal.class, paidInvoice).intValue());
        }
        jdbc.update("UPDATE classes SET tuition_fee=900000 WHERE class_id=?", sportsClass.getId());
        enrollments.promoteWaitlist(sportsClass.getId(), cashier.getId());
        assertEquals("Waitlisted", enrollmentStatus(second));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=? AND member_id=?",Integer.class,sportsClass.getId(),second.getId()));
        enrollments.promoteWaitlist(sportsClass.getId(), cashier.getId());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=?",Integer.class,sportsClass.getId()));
        assertNoMemberships();
    }

    @Test void advancedCannotUseEitherGenericEnrollmentRouteOrLegacyWaitlistPromotion() throws Exception {
        jdbc.update("UPDATE classes SET class_type='Advanced' WHERE class_id=?", sportsClass.getId());
        var session = login(member);
        for (String route : List.of(path(), "/api/enrollments")) {
            for (String method : List.of("Cash", "VNPay")) {
                mvc.perform(post(route).session(session).contentType("application/json")
                                .content(json.writeValueAsString(Map.of("classId", sportsClass.getId(), "paymentMethod", method))))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("USE_ADVANCED_REGISTRATION"));
            }
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE class_id=?", Integer.class, sportsClass.getId()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM class_enrollments WHERE class_id=?", Integer.class, sportsClass.getId()));
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            ClassEnrollment old = new ClassEnrollment(); old.setMember(em.getReference(User.class, member.getId()));
            old.setSportsClass(em.getReference(SportsClass.class, sportsClass.getId()));
            old.setStatus("Waitlisted"); old.setEnrolledAt(NOW.minusDays(1)); em.persist(old);
        });
        enrollments.promoteWaitlist(sportsClass.getId(), cashier.getId());
        assertEquals("Waitlisted", enrollmentStatus(member));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE class_id=?", Integer.class, sportsClass.getId()));
        assertNoMemberships();
    }

    @Test void legacyCatalogPurchasesExtensionsAndStatusWritesAreRetiredButReadsRemain() throws Exception {
        int[] legacy = new TransactionTemplate(transactions).execute(tx -> {
            var catalog = new com.sportscenter.membership.MembershipPackage();
            catalog.setName("Local retired catalog " + fixture); catalog.setStatus("Active");
            catalog.setPackageType("Normal"); catalog.setPrice(new BigDecimal("100000")); catalog.setDurationDays(30); em.persist(catalog);
            var sub = new com.sportscenter.membership.MemberPackage(); sub.setMembershipPackage(catalog);
            sub.setMember(em.getReference(User.class, member.getId())); sub.setStatus("Expired");
            sub.setStartDate(NOW.toLocalDate().minusDays(60)); sub.setEndDate(NOW.toLocalDate().minusDays(30)); em.persist(sub);
            return new int[]{catalog.getId(), sub.getSubscriptionId()};
        });
        var memberSession = login(member); var staff = login(cashier);
        String buy = json.writeValueAsString(Map.of("packageId",legacy[0],"paymentMethod","Cash"));
        for (String route : List.of("/api/members/me/subscriptions", "/api/member-packages/subscribe-me")) {
            mvc.perform(post(route).session(memberSession).contentType("application/json").content(buy))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LEGACY_MEMBERSHIP_RETIRED"));
        }
        for (String route : List.of("/api/members/"+member.getId()+"/packages", "/api/members/"+member.getId()+"/subscriptions")) {
            mvc.perform(post(route).session(staff).contentType("application/json").content(buy))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LEGACY_MEMBERSHIP_RETIRED"));
        }
        for (String route : List.of("/api/member-packages/"+legacy[1], "/api/subscriptions/"+legacy[1])) {
            mvc.perform(post(route+"/extend").session(staff).contentType("application/json").content("{\"extendDays\":15,\"reason\":\"No legacy revival\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LEGACY_MEMBERSHIP_RETIRED"));
            mvc.perform(patch(route+"/status").session(staff).contentType("application/json").content("{\"status\":\"Active\"}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LEGACY_MEMBERSHIP_RETIRED"));
        }
        String catalogBody="{\"name\":\"Retired\",\"durationDays\":30,\"price\":100000,\"status\":\"Active\",\"packageType\":\"Normal\",\"totalCredit\":12}";
        mvc.perform(post("/api/packages").session(staff).contentType("application/json").content(catalogBody))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LEGACY_MEMBERSHIP_RETIRED"));
        mvc.perform(put("/api/packages/"+legacy[0]).session(staff).contentType("application/json").content(catalogBody))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LEGACY_MEMBERSHIP_RETIRED"));
        mvc.perform(patch("/api/packages/"+legacy[0]+"/status").session(staff).contentType("application/json").content("{\"status\":\"Active\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LEGACY_MEMBERSHIP_RETIRED"));
        mvc.perform(get("/api/members/"+member.getId()+"/subscriptions").session(memberSession)).andExpect(status().isOk());
        assertEquals("Expired",jdbc.queryForObject("SELECT status FROM member_packages WHERE subscription_id=?",String.class,legacy[1]));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM member_packages WHERE member_id=?",Integer.class,member.getId()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE member_id=?",Integer.class,member.getId()));
    }
}
