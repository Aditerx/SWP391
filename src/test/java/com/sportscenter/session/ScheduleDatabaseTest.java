package com.sportscenter.session;

import com.sportscenter.audit.AuditService;
import com.sportscenter.center.CenterContext;
import com.sportscenter.enrollment.EnrollmentService;
import com.sportscenter.membership.*;
import com.sportscenter.room.*;
import com.sportscenter.subject.*;
import com.sportscenter.sportclass.*;
import com.sportscenter.specialization.*;
import com.sportscenter.user.*;
import com.sportscenter.user.Role;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated in-memory DB only: real Spring transactions, locks and repository queries. */
@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:schedule_safety;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never", "logging.level.org.hibernate=WARN", "logging.level.org.hibernate.SQL=OFF", "debug=false", "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SessionService.class, SessionSlotValidator.class, ScheduleWriteLock.class,
        NormalClassCreationService.class, SportsClassService.class, CenterContext.class, EnrollmentService.class, com.sportscenter.invoice.InvoiceService.class, ScheduleDatabaseTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScheduleDatabaseTest {
    @org.springframework.boot.test.context.TestConfiguration
    static class Config {
        @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-10-23T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")); }
        @Bean com.fasterxml.jackson.databind.ObjectMapper objectMapper() { return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules(); }
    }
    @MockBean AuditService audit;
    @Autowired EnrollmentService enrollments;
    @Autowired com.sportscenter.enrollment.ClassEnrollmentRepository enrollmentRepository;
    @MockBean com.sportscenter.notification.NotificationService notifications;
    @MockBean com.sportscenter.invoice.PaymentProperties paymentProperties;
    @MockBean com.sportscenter.invoice.VnPayGateway gateway;
    @Autowired com.sportscenter.invoice.InvoiceService invoiceService;
    @Autowired com.sportscenter.invoice.InvoiceRepository invoiceRepository;
    @Autowired SessionService sessions;
    @Autowired NormalClassCreationService normalClasses;
    @Autowired SessionRepository sessionRepository;
    @Autowired SportsClassRepository classes;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CoachRepository coaches;
    @Autowired SubjectRepository subjects;
    @Autowired RoomRepository rooms;
    @Autowired SpecializationRepository specializations;
    @Autowired CoachSpecializationRepository assignments;
    @Autowired MembershipPackageRepository packages;
    @Autowired MemberPackageRepository subscriptions;
    @Autowired NormalClassCreationRepository requests;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

    User coach, manager, memberA, memberB;
    Room room;
    Subject subject;
    SportsClass template;
    MemberPackage packageA, packageB;
    final LocalDate date = LocalDate.of(2026, 10, 26);

    @BeforeAll void seed() {
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> seedData());
    }
    void seedData() {
        assertTrue(jdbc.queryForObject("SELECT 1", Integer.class) == 1);
        jdbc.update("INSERT INTO centers(center_id) VALUES (1)");
        jdbc.execute("CREATE SEQUENCE invoice_code_seq START WITH 1");
        var coachRole = new Role(); coachRole.setName("Coach"); coachRole = roles.save(coachRole);
        var memberRole = new Role(); memberRole.setName("Member"); memberRole = roles.save(memberRole);
        var managerRole = new Role(); managerRole.setName("CenterManager"); managerRole = roles.save(managerRole);
        coach = user("coach@test.invalid", coachRole); manager = user("manager@test.invalid", managerRole);
        memberA = user("a@test.invalid", memberRole); memberB = user("b@test.invalid", memberRole);
        var profile = new Coach(); profile.setUser(coach); profile = coaches.save(profile);
        subject = new Subject(); subject.setName("Tennis"); subject.setSlug("quan-vot-tennis"); subject.setOutdoor(true);
        subject.setNormalPrice(new java.math.BigDecimal("2500000")); subject = subjects.save(subject);
        var spec = new Specialization(); spec.setName("Tennis"); spec.setSubject(subject); spec = specializations.save(spec);
        assignments.save(new CoachSpecialization(profile, spec));
        room = new Room(); room.setName("Test court"); room.setStatus("Available"); room.setCapacity(15); room.setFacilityType("Outdoor"); room = rooms.save(room);
        template = new SportsClass(); template.setName("Tennis private"); template.setSubject(subject); template.setClassType("Advanced"); template.setMaxCapacity(1); template.setStatus("Open"); template = classes.save(template);
        var pkg = new MembershipPackage(); pkg.setName("Test package"); pkg.setPackageType("Advanced"); pkg.setDurationDays(45); pkg.setPrice(java.math.BigDecimal.TEN); pkg.setStatus("Active"); pkg = packages.save(pkg);
        packageA = subscription(memberA, pkg); packageB = subscription(memberB, pkg);
    }
    User user(String email, Role role) {
        var u = new User(); u.setEmail(email); u.setFullName(email); u.setRole(role); u.setCenterId(1); u.setStatus("Active"); u.setPasswordHash("unused-test-only"); return users.save(u);
    }
    MemberPackage subscription(User user, MembershipPackage pkg) {
        var p = new MemberPackage(); p.setMember(user); p.setMembershipPackage(pkg); p.setStartDate(date.minusDays(3)); p.setEndDate(date.plusDays(40)); p.setRemainingCredit(12); p.setStatus("Active"); return subscriptions.save(p);
    }
    Authentication auth(User u, String authority) {
        return new UsernamePasswordAuthenticationToken(u.getEmail(), "", List.of(new SimpleGrantedAuthority(authority)));
    }
    PrivateBookingRequest booking(MemberPackage p) {
        return new PrivateBookingRequest(p.getSubscriptionId(), coach.getId(), room.getId(), date, LocalTime.of(6,0), LocalTime.of(7,30), template.getId(), subject.getId());
    }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }

    @Test void concurrentBookingsCannotShareCoachOrRoomAndCreditIsNotLost() throws Exception {
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var a = pool.submit(() -> attempt(packageA, memberA, start));
            var b = pool.submit(() -> attempt(packageB, memberB, start));
            start.countDown();
            assertEquals(1, (a.get(20, TimeUnit.SECONDS) ? 1 : 0) + (b.get(20, TimeUnit.SECONDS) ? 1 : 0));
            assertEquals(23, subscriptions.findById(packageA.getSubscriptionId()).orElseThrow().getRemainingCredit()
                    + subscriptions.findById(packageB.getSubscriptionId()).orElseThrow().getRemainingCredit());
            var booked = sessionRepository.findAll().stream().filter(s -> s.getBookedMember() != null && date.equals(s.getSessionDate()) && LocalTime.of(6,0).equals(s.getStartTime()) && "Scheduled".equals(s.getStatus())).findFirst().orElseThrow();
            User owner = users.findById(booked.getBookedMember().getId()).orElseThrow();
            var cancelStart = new CountDownLatch(1);
            Callable<Boolean> cancel = () -> { cancelStart.await(); try { sessions.cancelPrivateSession(booked.getId(),auth(owner,"ROLE_MEMBER")); return true; }
                catch (com.sportscenter.common.exception.BusinessException expected) { return false; } };
            var cancelA = pool.submit(cancel); var cancelB = pool.submit(cancel); cancelStart.countDown();
            assertEquals(1,(cancelA.get(20,TimeUnit.SECONDS)?1:0)+(cancelB.get(20,TimeUnit.SECONDS)?1:0));
            assertThrows(com.sportscenter.common.exception.BusinessException.class, () -> sessions.cancelPrivateSession(booked.getId(), auth(owner, "ROLE_MEMBER")));
            assertEquals(24, subscriptions.findById(packageA.getSubscriptionId()).orElseThrow().getRemainingCredit()
                    + subscriptions.findById(packageB.getSubscriptionId()).orElseThrow().getRemainingCredit());
        } finally { pool.shutdownNow(); }
    }
    boolean attempt(MemberPackage p, User u, CountDownLatch latch) throws Exception {
        latch.await();
        try { sessions.bookPrivateSession(booking(p), auth(u, "ROLE_MEMBER")); return true; }
        catch (com.sportscenter.common.exception.BusinessException expected) { return false; }
    }

    @Test void sameSubscriptionConcurrentDifferentSlotsDeductsBothCredits() throws Exception {
        var sub = subscription(memberA, packages.findAll().get(0));
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var futures = new ArrayList<Future<SessionResponse>>();
            for (int hour : List.of(6, 16)) {
                LocalTime from = hour == 6 ? LocalTime.of(6,0) : LocalTime.of(16,30);
                futures.add(pool.submit(() -> {
                    start.await();
                    return sessions.bookPrivateSession(new PrivateBookingRequest(sub.getSubscriptionId(), coach.getId(), room.getId(), date.plusDays(1), from, from.plusMinutes(90), template.getId(), subject.getId()), auth(memberA,"ROLE_MEMBER"));
                }));
            }
            start.countDown();
            for (var future : futures) assertNotNull(future.get(20, TimeUnit.SECONDS).id());
            assertEquals(10, subscriptions.findById(sub.getSubscriptionId()).orElseThrow().getRemainingCredit());
        } finally { pool.shutdownNow(); }
    }

    @Test void concurrentCashConfirmationsActivateCreditExactlyOnce() throws Exception {
        var role = new Role(); role.setName("Receptionist"); role = roles.save(role);
        var receptionist = user("cash@test.invalid",role);
        var sub = subscription(memberB, packages.findAll().get(0)); sub.setStatus("Pending"); sub.setRemainingCredit(0); sub = subscriptions.save(sub);
        var invoice = new com.sportscenter.invoice.Invoice(); invoice.setCreatedAt(LocalDateTime.of(2026,10,23,8,0)); invoice.setMember(memberB); invoice.setSubscription(sub);
        invoice.setMembershipPackage(packages.findAll().get(0)); invoice.setAmount(java.math.BigDecimal.TEN);
        invoice.setPaymentMethod("Cash"); invoice.setPaymentStatus("Pending"); invoice.setInvoiceCode("TEST-CASH-ONCE");
        invoice.setExpiresAt(LocalDateTime.of(2026,10,23,8,5)); invoice = invoiceRepository.save(invoice);
        final Integer id = invoice.getInvoiceId();
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        Callable<Boolean> confirm = () -> { start.await(); try { invoiceService.confirmCash(id,auth(receptionist,"ROLE_RECEPTIONIST")); return true; }
            catch (com.sportscenter.invoice.InvoiceConflictException expected) { return false; } };
        try {
            var a = pool.submit(confirm); var b = pool.submit(confirm); start.countDown();
            assertEquals(1,(a.get(20,TimeUnit.SECONDS)?1:0)+(b.get(20,TimeUnit.SECONDS)?1:0));
            assertEquals("Paid",invoiceRepository.findById(id).orElseThrow().getPaymentStatus());
            assertEquals(12,subscriptions.findById(sub.getSubscriptionId()).orElseThrow().getRemainingCredit());
        } finally { pool.shutdownNow(); }
    }

    @Test void lateVerifiedMoneyIsPreservedButCannotActivateSubscription() {
        var sub = subscription(memberB,packages.findAll().get(0)); sub.setStatus("Pending"); sub.setRemainingCredit(0); sub = subscriptions.save(sub);
        var invoice = new com.sportscenter.invoice.Invoice(); invoice.setCreatedAt(LocalDateTime.of(2026,10,23,8,0)); invoice.setMember(memberB); invoice.setSubscription(sub); invoice.setMembershipPackage(packages.findAll().get(0));
        invoice.setAmount(java.math.BigDecimal.TEN); invoice.setPaymentMethod("VNPay"); invoice.setPaymentStatus("Expired"); invoice.setInvoiceCode("TEST-LATE");
        invoice.setExpiresAt(LocalDateTime.of(2026,10,23,7,55)); invoice = invoiceRepository.save(invoice);
        var params = Map.of("vnp_TxnRef","TEST-LATE","vnp_Amount","1000","vnp_ResponseCode","00","vnp_TransactionStatus","00","vnp_TransactionNo","GATEWAY-LATE");
        org.mockito.Mockito.when(gateway.isValid(params)).thenReturn(true);
        assertEquals("00",invoiceService.processVnpayResult(params).code());
        var paid = invoiceRepository.findById(invoice.getInvoiceId()).orElseThrow();
        assertEquals("Paid",paid.getPaymentStatus()); assertTrue(paid.isReconciliationRequired());
        var state = subscriptions.findById(sub.getSubscriptionId()).orElseThrow();
        assertEquals("Cancelled",state.getStatus()); assertEquals(0,state.getRemainingCredit());
        final Integer subscriptionId = sub.getSubscriptionId();
        assertThrows(com.sportscenter.common.exception.BusinessException.class, () -> invoiceService.requirePaidSubscription(subscriptionId));
        assertEquals("02",invoiceService.processVnpayResult(params).code());
    }

    NormalClassCreationRequest normalRequest(UUID id, LocalTime time) {
        return new NormalClassCreationRequest(id, new SportsClassRequest("Normal test", subject.getId(), coach.getId(), 15,
                date, date.plusDays(40), "Open"), room.getId(), List.of(1,3,5), time, time.plusMinutes(90));
    }
    @Test void normalCreationIsAtomicAndRequestRetriesDoNotGenerateAnotherClass() {
        SecurityContextHolder.getContext().setAuthentication(auth(manager, "MANAGE_CLASSES"));
        long beforeRequests = requests.count();
        UUID key = UUID.randomUUID();
        var request = normalRequest(key, LocalTime.of(7,45));
        var result = normalClasses.create(request);
        assertEquals("Open", result.status());
        assertEquals(12, sessionRepository.countBySportsClassIdAndStatusNot(result.id(), "Cancelled"));
        assertEquals(result.id(), normalClasses.create(request).id());
        assertThrows(com.sportscenter.common.exception.BusinessException.class, () -> normalClasses.create(normalRequest(key, LocalTime.of(6,0))));
        long before = classes.count();
        assertThrows(com.sportscenter.common.exception.BusinessException.class, () -> normalClasses.create(normalRequest(UUID.randomUUID(), LocalTime.of(7,45))));
        assertEquals(before, classes.count(), "A schedule conflict must roll back the new class");
        assertEquals(beforeRequests + 1, requests.count());
    }

    @Test void twoMembersCompetingForLastNormalSeatCreateOnlyOnePendingInvoice() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(auth(manager,"MANAGE_CLASSES"));
        var request = new NormalClassCreationRequest(UUID.randomUUID(), new SportsClassRequest("Last seat",subject.getId(),coach.getId(),1,date,date.plusDays(40),"Open"),room.getId(),List.of(1,3,5),LocalTime.of(14,45),LocalTime.of(16,15));
        Integer classId = normalClasses.create(request).id();
        SecurityContextHolder.clearContext();
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var a = pool.submit(() -> { start.await(); return enrollments.enroll(classId,memberA.getId(),manager.getEmail(),"Cash"); });
            var b = pool.submit(() -> { start.await(); return enrollments.enroll(classId,memberB.getId(),manager.getEmail(),"Cash"); });
            start.countDown();
            var statuses = List.of(a.get(20,TimeUnit.SECONDS).status(),b.get(20,TimeUnit.SECONDS).status());
            assertEquals(1,statuses.stream().filter("Pending"::equals).count());
            assertEquals(1,statuses.stream().filter("Waitlisted"::equals).count());
            assertEquals(1,enrollmentRepository.countBySportsClassIdAndStatusIn(classId,List.of("Registered","Pending")));
            assertEquals(1,invoiceRepository.findAll().stream().filter(i -> i.getSportsClass() != null && classId.equals(i.getSportsClass().getId()) && "Pending".equals(i.getPaymentStatus())).count());
        } finally { pool.shutdownNow(); }
    }
}
