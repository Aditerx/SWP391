package com.sportscenter.invoice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.membership.*;
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
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** D16: real HTTP authentication, committed fixtures and post-request database reads. Local only. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "spring.config.import=", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.url=jdbc:h2:mem:cash_correction;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false", "spring.sql.init.mode=never",
        "vnpay.tmn-code=LOCALTEST", "vnpay.hash-secret=local-correction-test-key", "vnpay.pay-url=http://127.0.0.1:9/no-provider",
        "logging.level.root=WARN", "debug=false"})
@Import(PaymentResolutionCashCorrectionHttpTest.Time.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentResolutionCashCorrectionHttpTest {
    static final LocalDateTime NOW=LocalDateTime.of(2026,11,6,8,0);
    @TestConfiguration static class Time {
        @Bean @Primary Clock clock(){return Clock.fixed(NOW.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),ZoneId.of("Asia/Ho_Chi_Minh"));}
    }
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry r){
        String url=System.getProperty("scms.phases.pg-url","");
        if(!url.isBlank()){
            if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+"))throw new IllegalArgumentException("Disposable local PG only");
            r.add("spring.datasource.url",()->url);r.add("spring.datasource.username",()->"postgres");r.add("spring.datasource.password",()->"scms-local-only");
            r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");r.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect");r.add("spring.jpa.hibernate.ddl-auto",()->"none");
        }
    }
    @Autowired EntityManager em;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    @LocalServerPort int port;
    @MockBean PaymentExpirationJob paymentJob;
    @MockBean PackageExpirationJob packageJob;
    @MockBean AttendanceFinalizationJob attendanceJob;
    @MockBean NotificationScheduleJob notificationJob;
    Role memberRole,coachRole,managerRole,cashierRole;
    User member,coach,cashier,manager,foreignManager;
    HttpClient memberClient,cashierClient,managerClient,foreignClient;
    int fixture,subId,invoiceId,classId;

    @BeforeAll void roles(){
        tx.executeWithoutResult(t->{
            for(int center:List.of(1,2))if(jdbc.queryForObject("SELECT count(*) FROM centers WHERE center_id=?",Integer.class,center)==0){
                if(System.getProperty("scms.phases.pg-url","").isBlank())jdbc.update("INSERT INTO centers(center_id) VALUES(?)",center);
                else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(?,?)",center,"Local correction center "+center);
            }
            memberRole=role("Member");coachRole=role("Coach");managerRole=role("CenterManager","MANAGE_INVOICES");cashierRole=role("Receptionist","MANAGE_INVOICES","REGISTER_MEMBER");
        });
    }
    Role role(String name,String...authorities){
        Role r=em.createQuery("select r from Role r where r.name=:name",Role.class).setParameter("name",name).getResultStream().findFirst().orElseGet(()->{Role value=new Role();value.setName(name);em.persist(value);return value;});
        for(String nameOfPermission:authorities){
            Permission p=em.createQuery("select p from Permission p where p.name=:name",Permission.class).setParameter("name",nameOfPermission).getResultStream().findFirst().orElseGet(()->{Permission value=new Permission();value.setName(nameOfPermission);em.persist(value);return value;});
            r.getPermissions().add(p);
        }
        return r;
    }
    User user(String name,Role role,int center){
        User u=new User();u.setFullName(name);u.setEmail("correction-"+name+fixture+"@example.test");u.setRole(em.getReference(Role.class,role.getId()));u.setCenterId(center);u.setStatus("Active");u.setPasswordHash(passwords.encode("Local-Correction-2026!"));em.persist(u);return u;
    }
    @BeforeEach void committedFixture() throws Exception {
        fixture++;
        tx.executeWithoutResult(t->{
            member=user("member",memberRole,1);coach=user("coach",coachRole,1);cashier=user("cashier",cashierRole,1);manager=user("manager",managerRole,1);foreignManager=user("foreign",managerRole,2);
            Member profile=new Member();profile.setUser(member);profile.setJoinDate(NOW.toLocalDate());em.persist(profile);
            Coach cp=new Coach();cp.setUser(coach);em.persist(cp);Receptionist rp=new Receptionist();rp.setUser(cashier);em.persist(rp);
            Subject subject=em.createQuery("select s from Subject s where s.slug='cau-long'",Subject.class).getResultStream().findFirst().orElseGet(()->{
                Subject value=new Subject();value.setName("Cầu lông");value.setSlug("cau-long");value.setNormalPrice(new BigDecimal("900000"));value.setAdvancedPrice(new BigDecimal("3000000"));value.setOutdoor(false);value.setStatus("Active");em.persist(value);return value;
            });
            MemberPackage sub=new MemberPackage();sub.setMember(member);sub.setSubject(subject);sub.setPrimaryCoach(coach);sub.setStatus("Active");sub.setRemainingCredit(12);sub.setPurchaseAmount(new BigDecimal("3000000"));sub.setActivatedAt(NOW.minusDays(1));sub.setExpiresAt(NOW.plusDays(44));sub.setStartDate(NOW.minusDays(1).toLocalDate());sub.setEndDate(NOW.plusDays(44).toLocalDate());em.persist(sub);subId=sub.getSubscriptionId();
            // Synthetic fixture: mistaken Cash confirmation, no actual receipt. Never a live account/transaction.
            Invoice i=new Invoice();i.setMember(member);i.setSubscription(sub);i.setReceptionist(cashier);i.setAmount(sub.getPurchaseAmount());i.setInvoiceCode("CORR-"+fixture);i.setPaymentMethod("Cash");i.setPaymentStatus("Paid");i.setCreatedAt(NOW.minusDays(1));i.setPaymentDate(NOW.minusDays(1));em.persist(i);invoiceId=i.getInvoiceId();
            Room room=new Room();room.setActivityType(com.sportscenter.room.RoomActivity.BADMINTON);room.setName("Correction room "+fixture);room.setCapacity(15);room.setStatus("Available");room.setCenterId(1);room.setFacilityType("Indoor");em.persist(room);
            SportsClass c=new SportsClass();c.setName("Independent Normal "+fixture);c.setClassType("Normal");c.setSubject(subject);c.setCoach(coach);c.setStatus("Open");c.setCenterId(1);c.setMaxCapacity(15);c.setTuitionFee(new BigDecimal("900000"));em.persist(c);classId=c.getId();
            for(int day=1;day<=12;day++){Session s=new Session();s.setSportsClass(c);s.setCoach(coach);s.setRoom(room);s.setSessionDate(NOW.toLocalDate().plusDays(day+2));s.setStartTime(LocalTime.of(7,45));s.setEndTime(LocalTime.of(9,15));em.persist(s);}
        });
        memberClient=login(member);cashierClient=login(cashier);managerClient=login(manager);foreignClient=login(foreignManager);
    }
    HttpClient login(User actor) throws Exception {
        HttpClient c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        call(c,"POST","/api/auth/login",Map.of("email",actor.getEmail(),"password","Local-Correction-2026!"),200);
        JsonNode me=call(c,"GET","/api/auth/me",null,200);assertEquals(actor.getId(),me.path("userId").asInt());
        assertEquals(jdbc.queryForObject("SELECT r.role_name FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.user_id=?",String.class,actor.getId()),me.path("role").asText());return c;
    }
    JsonNode call(HttpClient actor,String method,String path,Object body,int status) throws Exception {
        HttpRequest request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        var response=actor.send(request,HttpResponse.BodyHandlers.ofString());assertEquals(status,response.statusCode(),method+" "+path+": "+response.body());return response.body().isBlank()?json.createObjectNode():json.readTree(response.body());
    }
    int normalOrder(boolean paid) throws Exception {
        jdbc.execute("CREATE SEQUENCE IF NOT EXISTS invoice_code_seq START WITH 20000");
        JsonNode e=paid?call(cashierClient,"POST","/api/classes/"+classId+"/enroll",Map.of("memberId",member.getId(),"paymentMethod","Cash"),201)
                :call(memberClient,"POST","/api/classes/"+classId+"/enroll",Map.of("paymentMethod","VNPay"),201);
        assertEquals(paid?"Registered":"Pending",e.path("status").asText());int id=e.path("invoiceId").asInt();
        assertNull(jdbc.queryForObject("SELECT eligibility_subscription_id FROM class_enrollments WHERE class_id=? AND member_id=?",Integer.class,classId,member.getId()));
        assertTrue(jdbc.queryForObject("SELECT enrolled_at FROM class_enrollments WHERE class_id=? AND member_id=?",LocalDateTime.class,classId,member.getId()).isAfter(NOW.minusDays(1)));
        return id;
    }
    int open() throws Exception {return call(cashierClient,"POST","/api/payment-resolutions",Map.of("invoiceId",invoiceId,"type","CashCorrection","reason","LOCAL TEST confirmed without receiving money"),200).path("id").asInt();}
    JsonNode approve(int id) throws Exception{return call(managerClient,"POST","/api/payment-resolutions/"+id+"/approve",null,200);}
    JsonNode complete(int id,boolean noMoney) throws Exception{return call(managerClient,"POST","/api/payment-resolutions/"+id+"/complete",Map.of("evidence","LOCAL TEST reconciled counter receipt","moneyNotReceived",noMoney),200);}
    Map<String,Object> invoice(int id){return jdbc.queryForMap("SELECT * FROM invoices WHERE invoice_id=?",id);}
    void assertOriginalUntouched(){assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoiceId));assertEquals(3000000,jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?",BigDecimal.class,invoiceId).intValueExact());}
    void assertUnreversed(){assertOriginalUntouched();assertFalse(jdbc.queryForObject("SELECT cash_confirmation_reversed FROM invoices WHERE invoice_id=?",Boolean.class,invoiceId));assertEquals("Active",jdbc.queryForObject("SELECT status FROM member_packages WHERE subscription_id=?",String.class,subId));assertEquals(12,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,subId));}

    void independentNormalDoesNotBlock(boolean paid) throws Exception {
        int normalInvoice=normalOrder(paid);var normalBefore=invoice(normalInvoice);var enrollmentBefore=jdbc.queryForList("SELECT * FROM class_enrollments WHERE class_id=?",classId);
        int id=open();assertEquals(id,open());assertEquals("Approved",approve(id).path("status").asText());
        // Even after approval, the no-receipt assertion is mandatory and failures must roll back.
        call(managerClient,"POST","/api/payment-resolutions/"+id+"/complete",Map.of("evidence","LOCAL TEST","moneyNotReceived",false),400);assertUnreversed();
        assertEquals("Completed",complete(id,true).path("status").asText());assertEquals("Completed",complete(id,true).path("status").asText());
        assertOriginalUntouched();assertTrue(jdbc.queryForObject("SELECT cash_confirmation_reversed FROM invoices WHERE invoice_id=?",Boolean.class,invoiceId));
        assertEquals("Cancelled",jdbc.queryForObject("SELECT status FROM member_packages WHERE subscription_id=?",String.class,subId));assertEquals(0,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,subId));
        assertEquals(normalBefore,invoice(normalInvoice));assertEquals(enrollmentBefore,jdbc.queryForList("SELECT * FROM class_enrollments WHERE class_id=?",classId));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM payment_resolutions WHERE invoice_id=?",Integer.class,invoiceId));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action='COMPLETE_PAYMENT_RESOLUTION' AND target_id=?",Integer.class,invoiceId));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE dedupe_key=?",Integer.class,"PAYMENT_RESOLUTION:"+id));
    }
    @Test void laterPendingNormalIsIndependent() throws Exception {independentNormalDoesNotBlock(false);}
    @Test void laterPaidNormalIsIndependent() throws Exception {independentNormalDoesNotBlock(true);}
    @Test void realEntitlementLinkStillBlocksApproval() throws Exception {
        normalOrder(true);jdbc.update("UPDATE class_enrollments SET eligibility_subscription_id=? WHERE class_id=?",subId,classId);
        int id=open();JsonNode result=approve(id);assertEquals("Blocked",result.path("status").asText());assertTrue(result.path("outcomeNote").asText().contains("depends on this subscription"));assertUnreversed();
    }
    @Test void completeRechecksDependencyAddedAfterApproval() throws Exception {
        normalOrder(true);int id=open();assertEquals("Approved",approve(id).path("status").asText());
        jdbc.update("UPDATE class_enrollments SET eligibility_subscription_id=? WHERE class_id=?",subId,classId);
        assertEquals("Blocked",complete(id,true).path("status").asText());assertUnreversed();
    }
    @Test void completeRechecksCreditAndDoesNotTreatApprovalAsReversal() throws Exception {
        int id=open();assertEquals("Approved",approve(id).path("status").asText());assertUnreversed();
        jdbc.update("UPDATE member_packages SET remaining_credit=11 WHERE subscription_id=?",subId);
        assertEquals("Blocked",complete(id,true).path("status").asText());assertOriginalUntouched();assertFalse(jdbc.queryForObject("SELECT cash_confirmation_reversed FROM invoices WHERE invoice_id=?",Boolean.class,invoiceId));assertEquals(11,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,subId));
    }
    @Test void wrongRoleOrCenterCannotApproveOrComplete() throws Exception {
        int id=open();for(HttpClient actor:List.of(memberClient,cashierClient,foreignClient))call(actor,"POST","/api/payment-resolutions/"+id+"/approve",null,403);
        assertEquals("Open",jdbc.queryForObject("SELECT status FROM payment_resolutions WHERE id=?",String.class,id));assertUnreversed();
        assertEquals("Approved",approve(id).path("status").asText());
        for(HttpClient actor:List.of(memberClient,cashierClient,foreignClient))call(actor,"POST","/api/payment-resolutions/"+id+"/complete",Map.of("evidence","LOCAL TEST","moneyNotReceived",true),403);
        assertUnreversed();
    }
}
