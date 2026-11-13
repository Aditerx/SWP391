package com.sportscenter.sportclass;

import com.fasterxml.jackson.databind.*;
import com.sportscenter.user.*;
import com.sportscenter.session.*;
import com.sportscenter.invoice.*;
import com.sportscenter.training.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Owner Normal requirements: real auth/me, transactions and committed readback; no network providers. */
@SpringBootTest(properties={"spring.config.import=", "spring.datasource.url=jdbc:h2:mem:normal_lifecycle;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false",
    "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false", "vnpay.tmn-code=LOCALTEST", "vnpay.hash-secret=local-normal-test-key",
    "vnpay.pay-url=http://127.0.0.1:9/no-provider", "vnpay.return-url=http://127.0.0.1:9/no-return"})
@AutoConfigureMockMvc @Import(NormalLifecycleHttpTest.Config.class) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NormalLifecycleHttpTest {
    static final ZoneId VN=ZoneId.of("Asia/Ho_Chi_Minh");
    static final LocalDateTime BASE=LocalDateTime.of(2026,11,10,8,0);
    static final String PASSWORD="Local-Normal-Only123!";
    static class MutableTime extends Clock {
        volatile Instant instant=BASE.atZone(VN).toInstant();
        public ZoneId getZone(){return VN;} public Clock withZone(ZoneId z){return Clock.fixed(instant,z);} public Instant instant(){return instant;}
        void at(LocalDateTime value){instant=value.atZone(VN).toInstant();}
    }
    @TestConfiguration static class Config { @Bean @Primary MutableTime phase1Clock(){return new MutableTime();} }
    @org.springframework.test.context.DynamicPropertySource static void postgres(org.springframework.test.context.DynamicPropertyRegistry p) {
        String url=System.getProperty("scms.normal.pg-url",""); if(url.isBlank())return;
        if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_normal_[a-z0-9_]+"))throw new IllegalArgumentException("Disposable loopback Normal DB required");
        p.add("spring.datasource.url",()->url);p.add("spring.datasource.username",()->"postgres");p.add("spring.datasource.password",()->"scms-local-only");
        p.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");p.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect");p.add("spring.jpa.hibernate.ddl-auto",()->"none");
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired EntityManager em; @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions; @Autowired PasswordEncoder passwords; @Autowired MutableTime clock;
    @Autowired NormalLifecycleService lifecycle; @Autowired AttendanceFinalizationProcessor finalizer;
    @Autowired com.sportscenter.normal.NormalChangeService changes;
    @Autowired com.sportscenter.normal.NormalRefundService refunds;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired com.sportscenter.enrollment.EnrollmentService enrollmentService;
    @Autowired InvoiceService invoiceService;
    @MockBean NormalLifecycleJob lifecycleJob; @MockBean PaymentExpirationJob paymentJob;
    @MockBean com.sportscenter.membership.PackageExpirationJob packageJob;
    @MockBean AttendanceFinalizationJob attendanceJob; @MockBean com.sportscenter.notification.NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.auth.EmailService email;
    Role managerRole,coachRole,memberRole,cashierRole; User manager,coach,member,second,third,cashier,outsider;
    com.sportscenter.subject.Subject subject; com.sportscenter.room.Room room; int fixture;
    void tx(Runnable r){new TransactionTemplate(transactions).executeWithoutResult(t->r.run());}
    Role role(String name,String...permissions){Role r=new Role();r.setName(name);em.persist(r);for(String name2:permissions){var p=em.createQuery("select p from Permission p where p.name=:name",Permission.class).setParameter("name",name2).getResultStream().findFirst().orElse(null);if(p==null){p=new Permission();p.setName(name2);em.persist(p);}r.getPermissions().add(p);}return r;}
    User user(String key,Role role,int center){User u=new User();u.setFullName(key);u.setEmail(key+fixture+"@normal.test.invalid");u.setRole(role);u.setCenterId(center);u.setStatus("Active");u.setPasswordHash(passwords.encode(PASSWORD));em.persist(u);return u;}
    @BeforeAll void baseline(){tx(()->{
        if(System.getProperty("scms.normal.pg-url","").isBlank()){jdbc.update("INSERT INTO centers(center_id) VALUES(1),(2)");jdbc.execute("CREATE SEQUENCE IF NOT EXISTS invoice_code_seq START WITH 1");}
        else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(2,'Normal isolated boundary fixture') ON CONFLICT(center_id) DO NOTHING");
        managerRole=role("CenterManager","MANAGE_CLASSES","MANAGE_INVOICES","VIEW_REPORTS");coachRole=role("Coach","RECORD_RESULT");memberRole=role("Member");cashierRole=role("Receptionist","REGISTER_MEMBER","MANAGE_INVOICES");
    });}
    @BeforeEach void fixture(){clock.at(BASE);fixture++;tx(()->{
        manager=user("manager",managerRole,1);coach=user("coach",coachRole,1);member=user("member",memberRole,1);second=user("second",memberRole,1);third=user("third",memberRole,1);cashier=user("cashier",cashierRole,1);outsider=user("outside",managerRole,2);
        for(User u:List.of(member,second,third)){Member p=new Member();p.setUser(u);p.setJoinDate(BASE.toLocalDate());em.persist(p);}
        var cp=new Coach();cp.setUser(coach);em.persist(cp);var rp=new Receptionist();rp.setUser(cashier);em.persist(rp);
        subject=em.createQuery("select s from Subject s where s.slug='cau-long'",com.sportscenter.subject.Subject.class).getResultStream().findFirst().orElse(null);
        if(subject==null){subject=new com.sportscenter.subject.Subject();subject.setName("Badminton");subject.setSlug("cau-long");subject.setOutdoor(false);subject.setNormalPrice(new BigDecimal("900000"));subject.setAdvancedPrice(new BigDecimal("3000000"));em.persist(subject);}
        var sp=new com.sportscenter.specialization.Specialization();sp.setName("Normal qualified "+fixture);sp.setSubject(subject);sp.setStatus("Active");em.persist(sp);em.persist(new com.sportscenter.specialization.CoachSpecialization(cp,sp));
        var work=new com.sportscenter.membership.CoachWorkSchedule();work.setCoach(coach);work.setAcceptingNew(true);for(DayOfWeek day:DayOfWeek.values())for(int slot=1;slot<=7;slot++)work.getWeeklySlots().add(day.name()+":"+slot);em.persist(work);
        room=new com.sportscenter.room.Room();room.setName("Normal court "+fixture);room.setCapacity(4);room.setStatus("Available");room.setFacilityType("Indoor");room.setActivityType(com.sportscenter.room.RoomActivity.BADMINTON);em.persist(room);
    });}
    @AfterAll void exportOpenApi()throws Exception{
        String output=System.getProperty("scms.normal.openapi","");if(output.isBlank())return;
        var spec=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        java.nio.file.Files.writeString(java.nio.file.Path.of(output),spec);
    }
    MockHttpSession login(User u)throws Exception{var result=mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",u.getEmail(),"password",PASSWORD)))).andExpect(status().isOk()).andReturn();var s=(MockHttpSession)result.getRequest().getSession(false);mvc.perform(get("/api/auth/me").session(s)).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(u.getId()));return s;}
    Map<String,Object> details(int capacity,Integer minimum){var d=new HashMap<String,Object>(Map.of("name","Normal "+fixture,"subjectId",subject.getId(),"coachId",coach.getId(),"maxCapacity",capacity,"classType","Normal","startDate",BASE.toLocalDate().plusDays(7).toString(),"status","Open"));if(minimum!=null)d.put("minCapacity",minimum);return d;}
    Map<String,Object> creation(int capacity,Integer minimum){return Map.of("requestId",UUID.randomUUID(),"sportsClass",details(capacity,minimum),"roomId",room.getId(),"daysOfWeek",List.of(1,3,5),"startTime","07:45","endTime","09:15");}
    JsonNode create(int capacity,Integer minimum)throws Exception{return json.readTree(mvc.perform(post("/api/classes/normal-with-sessions").session(login(manager)).contentType("application/json").content(json.writeValueAsString(creation(capacity,minimum)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());}
    LocalDateTime first(int id){return jdbc.queryForObject("SELECT session_date,start_time FROM sessions WHERE class_id=? ORDER BY session_date,start_time LIMIT 1",(r,n)->r.getDate(1).toLocalDate().atTime(r.getTime(2).toLocalTime()),id);}
    JsonNode enroll(int id,User target,String method)throws Exception{User actor="Cash".equals(method)?cashier:target;return json.readTree(mvc.perform(post("/api/classes/"+id+"/enroll").session(login(actor)).contentType("application/json").content(json.writeValueAsString(Map.of("memberId",target.getId(),"paymentMethod",method)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());}
    String state(int id){return jdbc.queryForObject("SELECT status FROM classes WHERE class_id=?",String.class,id);}
    @Test void minimumFormulaCreationRollbackAndImmutableAfterPending()throws Exception{
        assertArrayEquals(new int[]{2,3,4,6,7,10},java.util.stream.IntStream.of(2,4,5,8,10,15).map(NormalClassPolicy::minimum).toArray());
        mvc.perform(post("/api/classes/normal-with-sessions").session(login(manager)).contentType("application/json").content(json.writeValueAsString(creation(4,2)))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NORMAL_MIN_CAPACITY_INVALID"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM classes WHERE coach_id=?",Integer.class,coach.getId()));
        int id=create(4,null).path("id").asInt();assertEquals(3,jdbc.queryForObject("SELECT min_capacity FROM classes WHERE class_id=?",Integer.class,id));
        enroll(id,member,"VNPay");var d=details(4,4);d.put("startDate",first(id).toLocalDate().toString());d.put("endDate",jdbc.queryForObject("SELECT end_date FROM classes WHERE class_id=?",LocalDate.class,id).toString());
        mvc.perform(put("/api/classes/"+id).session(login(manager)).contentType("application/json").content(json.writeValueAsString(d))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NORMAL_MIN_CAPACITY_LOCKED"));
        assertEquals(3,jdbc.queryForObject("SELECT min_capacity FROM classes WHERE class_id=?",Integer.class,id));
    }
    @Test void cutoffUsesPersistedFirstSessionAndPendingNeverCountsForOpening()throws Exception{
        int id=create(4,null).path("id").asInt();enroll(id,member,"VNPay");
        mvc.perform(post("/api/classes/"+id+"/confirm-opening").session(login(manager))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NORMAL_INSUFFICIENT_PAID"));
        LocalDateTime first=first(id);clock.at(first.minusHours(48).minusMinutes(2));var order=enroll(id,second,"VNPay");
        assertEquals(first.minusHours(48),jdbc.queryForObject("SELECT expires_at FROM invoices WHERE invoice_id=?",LocalDateTime.class,order.path("invoiceId").asInt()));
        clock.at(first.minusHours(48));mvc.perform(post("/api/classes/"+id+"/enroll").session(login(third)).contentType("application/json").content("{\"paymentMethod\":\"VNPay\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NORMAL_REGISTRATION_CLOSED"));
        clock.at(first);lifecycle.reconcile(id);assertEquals("Open",state(id));assertNull(jdbc.queryForObject("SELECT opening_confirmed_at FROM classes WHERE class_id=?",LocalDateTime.class,id));
    }
    @Test void lateConfirmationRequiresReasonAndStartIsIdempotentAndStatusAliasesCannotCancel()throws Exception{
        int id=create(4,null).path("id").asInt();for(User u:List.of(member,second,third))enroll(id,u,"Cash");
        LocalDateTime first=first(id);clock.at(first.minusHours(24));
        mvc.perform(post("/api/classes/"+id+"/confirm-opening").session(login(manager))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REASON_REQUIRED"));
        mvc.perform(post("/api/classes/"+id+"/confirm-opening").session(login(outsider)).contentType("application/json").content("{\"reason\":\"Wrong center\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/classes/"+id+"/confirm-opening").session(login(cashier))).andExpect(status().isForbidden());
        var m=login(manager);String reason="{\"reason\":\"Late review completed before first session\"}";
        mvc.perform(post("/api/classes/"+id+"/confirm-opening").session(m).contentType("application/json").content(reason)).andExpect(status().isOk()).andExpect(jsonPath("$.openingConfirmedBy").value(manager.getId()));
        var at=jdbc.queryForObject("SELECT opening_confirmed_at FROM classes WHERE class_id=?",LocalDateTime.class,id);
        mvc.perform(post("/api/classes/"+id+"/confirm-opening").session(m).contentType("application/json").content(reason)).andExpect(status().isOk());assertEquals(at,jdbc.queryForObject("SELECT opening_confirmed_at FROM classes WHERE class_id=?",LocalDateTime.class,id));
        clock.at(first);lifecycle.reconcile(id);lifecycle.reconcile(id);assertEquals("Ongoing",state(id));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action='START_NORMAL_CLASS' AND target_id=?",Integer.class,id));
        for(String bad:List.of("Open","Closed","Cancelled")){var d=details(4,null);d.put("status",bad);d.put("startDate",first.toLocalDate().toString());d.put("endDate",jdbc.queryForObject("SELECT end_date FROM classes WHERE class_id=?",LocalDate.class,id).toString());mvc.perform(put("/api/classes/"+id).session(m).contentType("application/json").content(json.writeValueAsString(d))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NORMAL_LIFECYCLE_ACTION_REQUIRED"));assertEquals("Ongoing",state(id));}
    }
    @Test void twelveFinalizedSessionsCloseButKeepCoachCorrectionUntilMidnight()throws Exception{
        int id=create(1,null).path("id").asInt();enroll(id,member,"Cash");
        mvc.perform(post("/api/classes/"+id+"/confirm-opening").session(login(manager))).andExpect(status().isOk());clock.at(first(id));lifecycle.reconcile(id);
        var rows=jdbc.queryForList("SELECT session_id,session_date,end_time FROM sessions WHERE class_id=? ORDER BY session_date,start_time",id);
        for(var r:rows){clock.at(((java.sql.Date)r.get("session_date")).toLocalDate().atTime(((java.sql.Time)r.get("end_time")).toLocalTime()));finalizer.process((Integer)r.get("session_id"));}
        assertEquals("Closed",state(id));assertEquals("Completed",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",String.class,id,member.getId()));
        int sid=(Integer)rows.get(11).get("session_id");
        mvc.perform(post("/api/attendances/correct").session(login(coach)).contentType("application/json").content(json.writeValueAsString(Map.of("sessionId",sid,"memberId",member.getId(),"newState","Present","reason","Checked final session")))).andExpect(status().isOk());
        assertEquals("Present",jdbc.queryForObject("SELECT state FROM attendances WHERE session_id=? AND member_id=? AND superseded_at IS NULL",String.class,sid,member.getId()));
    }

    @Test void signedMoneyArrivingAtCutoffIsReconciledWithoutRevivingAPlaceAndReturnNeverPays()throws Exception{
        int id=create(1,null).path("id").asInt();LocalDateTime cutoff=first(id).minusHours(48);clock.at(cutoff.minusMinutes(2));
        var order=enroll(id,member,"VNPay");int invoice=order.path("invoiceId").asInt();
        var params=new TreeMap<String,String>(Map.of("vnp_TmnCode","LOCALTEST","vnp_TxnRef",order.path("invoiceCode").asText(),"vnp_Amount","90000000","vnp_ResponseCode","00","vnp_TransactionStatus","00","vnp_TransactionNo","NORMAL-LATE-"+fixture));
        String data=params.entrySet().stream().map(e->java.net.URLEncoder.encode(e.getKey(),java.nio.charset.StandardCharsets.UTF_8)+"="+java.net.URLEncoder.encode(e.getValue(),java.nio.charset.StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        var mac=javax.crypto.Mac.getInstance("HmacSHA512");mac.init(new javax.crypto.spec.SecretKeySpec("local-normal-test-key".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA512"));String query=data+"&vnp_SecureHash="+HexFormat.of().formatHex(mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(get("/api/payments/vnpay/return?"+query)).andExpect(status().is3xxRedirection());
        assertEquals("Pending",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoice));
        clock.at(cutoff);mvc.perform(get("/api/payments/vnpay/ipn?"+query)).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("00"));
        mvc.perform(get("/api/payments/vnpay/ipn?"+query)).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("02"));
        assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoice));
        assertTrue(jdbc.queryForObject("SELECT reconciliation_required FROM invoices WHERE invoice_id=?",Boolean.class,invoice));
        assertEquals("Cancelled",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",String.class,id,member.getId()));
        assertEquals(900000,jdbc.queryForObject("SELECT amount FROM invoices WHERE invoice_id=?",BigDecimal.class,invoice).intValue());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=?",Integer.class,id));
    }
    @Test void expiredHoldReleasesItsSeatInTheNextAdmissionTransaction()throws Exception{
        int id=create(1,null).path("id").asInt();var first=enroll(id,member,"VNPay");
        clock.at(BASE.plusMinutes(5));var next=enroll(id,second,"VNPay");
        assertEquals("Pending",next.path("status").asText());
        assertEquals("Expired",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,first.path("invoiceId").asInt()));
        assertEquals("Cancelled",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",String.class,id,member.getId()));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM class_enrollments WHERE class_id=? AND status='Pending'",Integer.class,id));
    }
    @Test void concurrentLastSeatKeepsOneOrderAndOneWaitlistAfterCommit()throws Exception{
        int id=create(1,null).path("id").asInt();var a=login(member);var b=login(second);
        var ready=new java.util.concurrent.CountDownLatch(2);var start=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try{
            var pending=new ArrayList<java.util.concurrent.Future<String>>();
            for(var session:List.of(a,b))pending.add(pool.submit(()->{ready.countDown();assertTrue(start.await(10,java.util.concurrent.TimeUnit.SECONDS));return json.readTree(mvc.perform(post("/api/classes/"+id+"/enroll").session(session).contentType("application/json").content("{\"paymentMethod\":\"VNPay\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("status").asText();}));
            assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS));start.countDown();var states=new ArrayList<String>();for(var r:pending)states.add(r.get(30,java.util.concurrent.TimeUnit.SECONDS));Collections.sort(states);assertEquals(List.of("Pending","Waitlisted"),states);
        }finally{start.countDown();pool.shutdownNow();}
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=?",Integer.class,id));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM class_enrollments WHERE class_id=? AND status='Pending'",Integer.class,id));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM class_enrollments WHERE class_id=? AND status='Waitlisted'",Integer.class,id));
    }
    JsonNode postAs(String path,User actor,Object body,int expected)throws Exception {
        return json.readTree(mvc.perform(post(path).session(login(actor)).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());
    }
    int cancelledRefund()throws Exception {
        int c=create(1,null).path("id").asInt();enroll(c,member,"Cash");
        postAs("/api/classes/"+c+"/cancel-before-opening",manager,Map.of("version",0,"reason","Center cannot provide the class before start"),200);
        return jdbc.queryForObject("SELECT r.id FROM normal_refunds r WHERE r.class_id=?",Integer.class,c);
    }
    JsonNode refund(int id,User actor)throws Exception{return json.readTree(mvc.perform(get("/api/normal-refunds/"+id).session(login(actor))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
    JsonNode approveRefund(int id)throws Exception {
        postAs("/api/normal-refunds/"+id+"/receiver",member,Map.of("version",0,"bank","DEMO BANK","accountNumber","DEMOACCOUNT123","accountName","DEMO Member"),200);
        postAs("/api/normal-refunds/"+id+"/verify-receiver",manager,Map.of("version",1,"source","DEMO Member ownership and account independently checked"),200);
        return postAs("/api/normal-refunds/"+id+"/approve",manager,Map.of("version",2),200);
    }
    @Test void refundApprovalRoleAuthorityCenterMatrixPreservesRejectedState()throws Exception {
        int id=cancelledRefund();
        postAs("/api/normal-refunds/"+id+"/receiver",member,Map.of("version",0,"bank","DEMO BANK","accountNumber","DEMOACCOUNT123","accountName","DEMO Member"),200);
        postAs("/api/normal-refunds/"+id+"/verify-receiver",manager,Map.of("version",1,"source","DEMO independent verification"),200);
        User admin= new org.springframework.transaction.support.TransactionTemplate(transactions).execute(t->user("admin-matrix",role("Admin","MANAGE_USERS","MANAGE_PERMISSIONS","MANAGE_INVOICES"),1));
        tx(()->{var permission=em.createQuery("select p from Permission p where p.name='MANAGE_INVOICES'",Permission.class).getSingleResult();em.find(Role.class,memberRole.getId()).getPermissions().add(permission);em.find(Role.class,coachRole.getId()).getPermissions().add(permission);});
        var before=List.of(jdbc.queryForList("SELECT * FROM normal_refunds"),jdbc.queryForList("SELECT * FROM payment_resolutions"),jdbc.queryForList("SELECT * FROM normal_refund_receivers"),jdbc.queryForList("SELECT * FROM invoices"),jdbc.queryForList("SELECT * FROM system_logs"));
        try {
            for(User actor:List.of(admin,coach,member,cashier,outsider)){
                postAs("/api/normal-refunds/"+id+"/approve",actor,Map.of("version",2),403);
                assertEquals(before,List.of(jdbc.queryForList("SELECT * FROM normal_refunds"),jdbc.queryForList("SELECT * FROM payment_resolutions"),jdbc.queryForList("SELECT * FROM normal_refund_receivers"),jdbc.queryForList("SELECT * FROM invoices"),jdbc.queryForList("SELECT * FROM system_logs")));
            }
            var session=login(manager);
            tx(()->em.find(Role.class,managerRole.getId()).getPermissions().removeIf(p->"MANAGE_INVOICES".equals(p.getName())));
            mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.authorities",org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("MANAGE_INVOICES"))));
            mvc.perform(post("/api/normal-refunds/"+id+"/approve").session(session).contentType("application/json").content("{\"version\":2}")).andExpect(status().isForbidden());
            assertEquals(before,List.of(jdbc.queryForList("SELECT * FROM normal_refunds"),jdbc.queryForList("SELECT * FROM payment_resolutions"),jdbc.queryForList("SELECT * FROM normal_refund_receivers"),jdbc.queryForList("SELECT * FROM invoices"),jdbc.queryForList("SELECT * FROM system_logs")));
        } finally {
            tx(()->{var permission=em.createQuery("select p from Permission p where p.name='MANAGE_INVOICES'",Permission.class).getSingleResult();em.find(Role.class,managerRole.getId()).getPermissions().add(permission);em.find(Role.class,memberRole.getId()).getPermissions().removeIf(p->"MANAGE_INVOICES".equals(p.getName()));em.find(Role.class,coachRole.getId()).getPermissions().removeIf(p->"MANAGE_INVOICES".equals(p.getName()));});
        }
        var approved=postAs("/api/normal-refunds/"+id+"/approve",manager,Map.of("version",2),200);assertEquals("Approved",approved.path("status").asText());assertEquals(3,approved.path("version").asLong());
        assertEquals("Approved",jdbc.queryForObject("SELECT p.status FROM payment_resolutions p JOIN normal_refunds r ON r.id=p.id WHERE r.id=?",String.class,id));
        var after=jdbc.queryForList("SELECT * FROM system_logs");postAs("/api/normal-refunds/"+id+"/approve",manager,Map.of("version",2),409);assertEquals(after,jdbc.queryForList("SELECT * FROM system_logs"));
    }
    @Test void cancellationCreatesEveryReceiptObligationAndRejectsOngoingAndRoleAliases()throws Exception {
        int c=create(4,null).path("id").asInt();var a=enroll(c,member,"Cash");enroll(c,second,"Cash");enroll(c,third,"VNPay");
        postAs("/api/classes/"+c+"/cancel-before-opening",cashier,Map.of("version",0,"reason","Cancel"),403);
        postAs("/api/classes/"+c+"/cancel-before-opening",manager,Map.of("version",0,"reason","Center cannot supply any sessions"),200);
        postAs("/api/classes/"+c+"/cancel-before-opening",manager,Map.of("version",0,"reason","Retry"),200);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM normal_refunds WHERE class_id=?",Integer.class,c));
        assertEquals(12,jdbc.queryForObject("SELECT count(*) FROM sessions WHERE class_id=? AND status='Cancelled'",Integer.class,c));
        assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,a.path("invoiceId").asInt()));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM class_enrollments WHERE class_id=? AND status<>'Cancelled'",Integer.class,c));
        int other=create(1,null).path("id").asInt();enroll(other,member,"Cash");postAs("/api/classes/"+other+"/confirm-opening",manager,Map.of(),200);clock.at(first(other));lifecycle.reconcile(other);
        postAs("/api/classes/"+other+"/cancel-before-opening",manager,Map.of("version",2,"reason","Not permitted after start"),400);assertEquals("Ongoing",state(other));
    }
    @Test void refundRequiresVerifiedReceiverClaimPrivateProofAndIndependentBankMatch()throws Exception {
        int id=cancelledRefund();
        postAs("/api/normal-refunds/"+id+"/claim",cashier,Map.of("version",0,"requestId",UUID.randomUUID()),400);
        mvc.perform(get("/api/normal-refunds/"+id).session(login(second))).andExpect(status().isForbidden());
        postAs("/api/normal-refunds/"+id+"/receiver",member,Map.of("version",0,"bank","DEMO BANK","accountNumber","DEMOACCOUNT123","accountName","DEMO Member"),200);
        postAs("/api/normal-refunds/"+id+"/approve",manager,Map.of("version",1),400);
        postAs("/api/normal-refunds/"+id+"/verify-receiver",manager,Map.of("version",1,"source","DEMO independent account verification"),200);
        var approved=postAs("/api/normal-refunds/"+id+"/approve",manager,Map.of("version",2),200);int receiver=approved.path("receiverId").asInt();UUID key=UUID.randomUUID();
        int attempt=postAs("/api/normal-refunds/"+id+"/claim",cashier,Map.of("version",3,"requestId",key),200).path("id").asInt();
        assertEquals(attempt,postAs("/api/normal-refunds/"+id+"/claim",cashier,Map.of("version",3,"requestId",key),200).path("id").asInt());
        postAs("/api/normal-refunds/"+id+"/claim",cashier,Map.of("version",4,"requestId",UUID.randomUUID()),400);
        String base="/api/normal-refunds/"+id+"/attempts/"+attempt;
        postAs(base+"/submit",cashier,Map.of("reference","DEMO-"+fixture,"amount",900000,"currency","VND","performedAt",BASE.toString()),200);
        Map<String,Object> verify=new HashMap<>(Map.of("version",5,"outcome","Success","bankSource","DEMO independent center statement","reference","DEMO-"+fixture,"amount",900000,"receiverId",receiver,"performedAt",BASE.toString(),"note","DEMO statement matched"));
        postAs(base+"/verify",manager,verify,400);
        var file=new org.springframework.mock.web.MockMultipartFile("file","proof.pdf","application/pdf","%PDF-1.4 DEMO ONLY proof".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        int doc=Integer.parseInt(mvc.perform(multipart(base+"/documents").file(file).session(login(cashier))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("AwaitingVerification",refund(id,member).path("status").asText());
        mvc.perform(get("/api/normal-refunds/"+id+"/documents/"+doc).session(login(member))).andExpect(status().isForbidden());
        mvc.perform(get("/api/normal-refunds/"+id+"/documents/"+doc).session(login(outsider))).andExpect(status().isForbidden());
        mvc.perform(get("/api/normal-refunds/"+id+"/documents/"+doc).session(login(manager))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store, private"));
        postAs(base+"/verify",cashier,verify,403);verify.put("amount",1);postAs(base+"/verify",manager,verify,400);verify.put("amount",900000);
        postAs(base+"/verify",manager,verify,200);postAs(base+"/verify",manager,verify,200);
        assertEquals("Completed",refund(id,member).path("status").asText());
        mvc.perform(get("/api/normal-refunds/"+id+"/history").session(login(member))).andExpect(status().isForbidden());
        mvc.perform(get("/api/normal-refunds/"+id+"/history").session(login(manager))).andExpect(status().isOk()).andExpect(jsonPath("$.verifications[0].outcome").value("Success"));
        assertTrue(refund(id,member).path("accountNumber").asText().startsWith("****"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM normal_refund_attempts WHERE refund_id=?",Integer.class,id));
        assertEquals("Paid",jdbc.queryForObject("SELECT i.payment_status FROM invoices i JOIN payment_resolutions r ON r.invoice_id=i.invoice_id WHERE r.id=?",String.class,id));
        postAs("/api/payment-resolutions/"+id+"/complete",manager,Map.of("evidence","No alternate completion","performedAt",BASE.toString()),400);
    }
    @Test void receiverChangesInvalidateApprovalAndSlaCountsCenterOpenWeekendsAndHoliday()throws Exception {
        int id=cancelledRefund();var a=approveRefund(id);String original=a.path("firstDueAt").asText();
        postAs("/api/normal-refunds/"+id+"/receiver",member,Map.of("version",3,"bank","DEMO BANK2","accountNumber","DEMOACCOUNT456","accountName","DEMO Member2"),200);
        assertEquals("AwaitingApproval",refund(id,manager).path("status").asText());
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM normal_refund_receivers WHERE refund_id=?",Integer.class,id));
        postAs("/api/normal-refunds/"+id+"/claim",cashier,Map.of("version",4,"requestId",UUID.randomUUID()),400);
        assertEquals(original,refund(id,manager).path("firstDueAt").asText());
        tx(()->{var h=new com.sportscenter.holiday.Holiday();h.setHolidayDate(BASE.toLocalDate().plusDays(1));h.setHolidayName("DEMO closed day");h.setActive(true);em.persist(h);});
        assertEquals(BASE.toLocalDate().plusDays(8),refunds.due(BASE).toLocalDate());
    }
    @Test void postponementNeedsAllConsentsKeepsInvoiceAndOnlyAppliesOnce()throws Exception {
        int c=create(4,null).path("id").asInt();var order=enroll(c,member,"Cash");LocalDateTime original=first(c);
        var p=postAs("/api/classes/"+c+"/normal-proposals",manager,Map.of("kind","Postpone","shiftDays",35,"version",0,"reason","Insufficient paid enrollment"),200);int proposal=p.path("id").asInt();
        postAs("/api/normal-proposals/"+proposal+"/apply",manager,Map.of("version",1),400);
        postAs("/api/classes/"+c+"/confirm-opening",manager,Map.of(),400);
        postAs("/api/normal-proposals/"+proposal+"/respond",member,Map.of("version",0,"fingerprint",p.path("fingerprint").asText(),"choice","Accept"),409);
        var choice=Map.of("version",1,"fingerprint",p.path("fingerprint").asText(),"choice","Accept");postAs("/api/normal-proposals/"+proposal+"/respond",member,choice,200);postAs("/api/normal-proposals/"+proposal+"/respond",member,choice,200);
        postAs("/api/normal-proposals/"+proposal+"/apply",manager,Map.of("version",1),200);assertEquals(original.plusDays(35),first(c));
        assertEquals(1,jdbc.queryForObject("SELECT postponement_count FROM classes WHERE class_id=?",Integer.class,c));
        assertEquals(c,jdbc.queryForObject("SELECT class_id FROM invoices WHERE invoice_id=?",Integer.class,order.path("invoiceId").asInt()));
        postAs("/api/classes/"+c+"/normal-proposals",manager,Map.of("kind","Postpone","shiftDays",7,"version",2,"reason","Still insufficient"),400);
    }
    @Test void transferKeepsPurchasedClassRechecksLastSeatAndRefundCannotAlsoTransfer()throws Exception {
        int source=create(4,null).path("id").asInt();var order=enroll(source,member,"Cash");enroll(source,second,"Cash");
        var payload=new HashMap<>(creation(1,null));var details=details(1,null);details.put("startDate",BASE.toLocalDate().plusDays(49).toString());payload.put("sportsClass",details);
        int target=postAs("/api/classes/normal-with-sessions",manager,payload,201).path("id").asInt();
        var p=postAs("/api/classes/"+source+"/normal-proposals",manager,Map.of("kind","Transfer","targetClassId",target,"version",0,"reason","Center cannot organize original class"),200);int proposal=p.path("id").asInt();var accept=Map.of("version",1,"fingerprint",p.path("fingerprint").asText(),"choice","Accept");
        postAs("/api/normal-proposals/"+proposal+"/respond",member,accept,200);
        postAs("/api/normal-proposals/"+proposal+"/respond",second,accept,409);
        postAs("/api/normal-proposals/"+proposal+"/respond",second,Map.of("version",1,"fingerprint",p.path("fingerprint").asText(),"choice","Refund"),200);
        postAs("/api/normal-proposals/"+proposal+"/respond",second,accept,409);
        int invoice=order.path("invoiceId").asInt();assertEquals(source,jdbc.queryForObject("SELECT class_id FROM invoices WHERE invoice_id=?",Integer.class,invoice));assertEquals(target,jdbc.queryForObject("SELECT entitlement_class_id FROM invoices WHERE invoice_id=?",Integer.class,invoice));
        tx(()->{assertTrue(invoiceRepository.existsByMemberIdAndSportsClassIdAndPaymentStatusAndReconciliationRequiredFalseAndCashConfirmationReversedFalse(member.getId(),target,"Paid"));});
        assertEquals("Cancelled",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",String.class,source,member.getId()));
        assertEquals("Registered",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",String.class,target,member.getId()));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM normal_transfers WHERE invoice_id=?",Integer.class,invoice));
        tx(()->{var plan=new TrainingPlan();plan.setCoach(coach);plan.setSportsClass(em.getReference(SportsClass.class,target));plan.setTitle("Transferred Member plan");plan.setCreatedAt(BASE);em.persist(plan);});
        mvc.perform(get("/api/training-plans").param("classId",String.valueOf(target)).session(login(member))).andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Transferred Member plan"));
    }
    @Test void concurrentRefundClaimsCreateOneBankTaskAndNeverAutoRetryUnknownOutcome()throws Exception {
        int id=cancelledRefund();approveRefund(id);var a=login(cashier);var b=login(cashier);
        var start=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var calls=new ArrayList<java.util.concurrent.Future<Integer>>();for(var session:List.of(a,b))calls.add(pool.submit(()->{assertTrue(start.await(10,java.util.concurrent.TimeUnit.SECONDS));return mvc.perform(post("/api/normal-refunds/"+id+"/claim").session(session).contentType("application/json").content(json.writeValueAsString(Map.of("version",3,"requestId",UUID.randomUUID())))).andReturn().getResponse().getStatus();}));
            start.countDown();var states=new ArrayList<Integer>();for(var call:calls)states.add(call.get(30,java.util.concurrent.TimeUnit.SECONDS));Collections.sort(states);assertEquals(List.of(200,409),states);
        } finally {start.countDown();pool.shutdownNow();}
        int attempt=jdbc.queryForObject("SELECT id FROM normal_refund_attempts WHERE refund_id=?",Integer.class,id);
        postAs("/api/normal-refunds/"+id+"/attempts/"+attempt+"/verify",manager,Map.of("version",4,"outcome","Pending","bankSource","DEMO bank still pending","note","Check bank before retrying"),200);
        postAs("/api/normal-refunds/"+id+"/receiver",member,Map.of("version",5,"bank","DEMO BANK","accountNumber","DEMOACCOUNT456","accountName","DEMO Member"),409);
        postAs("/api/normal-refunds/"+id+"/claim",cashier,Map.of("version",5,"requestId",UUID.randomUUID()),400);
        assertEquals("NeedsReview",refund(id,member).path("status").asText());
        postAs("/api/normal-refunds/"+id+"/attempts/"+attempt+"/verify",manager,Map.of("version",5,"outcome","Failed","bankSource","DEMO bank confirmed debit never settled","note","Bank failure confirmed independently"),200);
        postAs("/api/normal-refunds/"+id+"/approve",manager,Map.of("version",6),200);
        postAs("/api/normal-refunds/"+id+"/claim",cashier,Map.of("version",7,"requestId",UUID.randomUUID()),200);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM normal_refund_attempts WHERE refund_id=?",Integer.class,id));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM normal_refund_verifications WHERE attempt_id=?",Integer.class,attempt));
    }
    @Test void informationCorrectionKeepsHistoryAndDoesNotEraseRefundObligation()throws Exception {
        int id=cancelledRefund();approveRefund(id);
        postAs("/api/normal-refunds/"+id+"/request-information",manager,Map.of("version",3,"note","Receiver account confirmation needs correction"),200);
        assertEquals("AwaitingReceiver",refund(id,member).path("status").asText());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM normal_refund_receivers WHERE refund_id=?",Integer.class,id));
        postAs("/api/normal-refunds/"+id+"/claim",cashier,Map.of("version",4,"requestId",UUID.randomUUID()),400);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM normal_refunds WHERE id=?",Integer.class,id));
    }
    @Test void expiredWaitlistOrderRotatesToTailAndInvitesTheNextMemberWithoutAutoCash()throws Exception {
        int c=create(1,null).path("id").asInt();int firstInvoice=enroll(c,member,"VNPay").path("invoiceId").asInt();
        enroll(c,second,"VNPay");enroll(c,third,"VNPay");clock.at(BASE.plusMinutes(6));invoiceService.expireInvoice(firstInvoice);enrollmentService.promoteWaitlist(c,null);
        assertNotNull(jdbc.queryForObject("SELECT promoted_at FROM class_enrollments WHERE class_id=? AND member_id=?",LocalDateTime.class,c,second.getId()));
        int next=enroll(c,second,"VNPay").path("invoiceId").asInt();clock.at(BASE.plusMinutes(12));invoiceService.expireInvoice(next);enrollmentService.promoteWaitlist(c,null);
        assertEquals("Waitlisted",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=? AND member_id=?",String.class,c,second.getId()));
        assertNotNull(jdbc.queryForObject("SELECT promoted_at FROM class_enrollments WHERE class_id=? AND member_id=?",LocalDateTime.class,c,third.getId()));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM invoices WHERE class_id=? AND payment_method='Cash'",Integer.class,c));
    }
    JsonNode revenue(String from,String to)throws Exception {return json.readTree(mvc.perform(get("/api/reports/revenue").param("startDate",from).param("endDate",to).session(login(manager))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
    @Test void verifiedRefundIsReportedInTransferPeriodAndDoesNotRewriteOriginalReceiptMonth()throws Exception {
        clock.at(BASE.minusMonths(1));int id=cancelledRefund();var approved=approveRefund(id);int receiver=approved.path("receiverId").asInt();
        int attempt=postAs("/api/normal-refunds/"+id+"/claim",cashier,Map.of("version",3,"requestId",UUID.randomUUID()),200).path("id").asInt();clock.at(BASE);
        JsonNode oct=revenue("2026-10-01","2026-10-31"),nov=revenue("2026-11-01","2026-11-30");
        String base="/api/normal-refunds/"+id+"/attempts/"+attempt;
        postAs(base+"/submit",cashier,Map.of("reference","DEMO-PERIOD-"+fixture,"amount",900000,"currency","VND","performedAt",BASE.toString()),200);
        var file=new org.springframework.mock.web.MockMultipartFile("file","proof.pdf","application/pdf","%PDF-1.4 DEMO ONLY".getBytes(java.nio.charset.StandardCharsets.UTF_8));mvc.perform(multipart(base+"/documents").file(file).session(login(cashier))).andExpect(status().isOk());
        var body=Map.of("version",5,"outcome","Success","bankSource","DEMO independent November bank statement","reference","DEMO-PERIOD-"+fixture,"amount",900000,"receiverId",receiver,"performedAt",BASE.toString(),"note","DEMO matched successful transfer");
        postAs(base+"/verify",manager,body,200);postAs(base+"/verify",manager,body,200);
        JsonNode afterOct=revenue("2026-10-01","2026-10-31"),afterNov=revenue("2026-11-01","2026-11-30");
        assertEquals(0,oct.path("moneyReceived").decimalValue().compareTo(afterOct.path("moneyReceived").decimalValue()));
        assertEquals(0,afterNov.path("moneyRefunded").decimalValue().subtract(nov.path("moneyRefunded").decimalValue()).compareTo(new BigDecimal("900000")));
        assertEquals(0,nov.path("moneyReceived").decimalValue().compareTo(afterNov.path("moneyReceived").decimalValue()));
        postAs("/api/normal-refunds/"+id+"/issues",member,Map.of("version",6,"note","DEMO Member has not received funds"),200);
        postAs(base+"/verify",manager,Map.of("version",7,"outcome","Reversed","bankSource","DEMO bank statement confirms returned funds","note","Transfer reversed; preserve the obligation"),200);
        assertEquals("AwaitingApproval",refund(id,member).path("status").asText());
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM normal_refund_verifications WHERE attempt_id=?",Integer.class,attempt));
    }
    @Test void signedReceiptAfterCenterCancellationCreatesOneRefundObligation()throws Exception {
        int c=create(1,null).path("id").asInt();var order=enroll(c,member,"VNPay");int invoice=order.path("invoiceId").asInt();
        postAs("/api/classes/"+c+"/cancel-before-opening",manager,Map.of("version",0,"reason","Center cannot provide class"),200);
        var params=new TreeMap<String,String>(Map.of("vnp_TmnCode","LOCALTEST","vnp_TxnRef",order.path("invoiceCode").asText(),"vnp_Amount","90000000","vnp_ResponseCode","00","vnp_TransactionStatus","00","vnp_TransactionNo","NORMAL-CANCEL-"+fixture));
        String data=params.entrySet().stream().map(e->java.net.URLEncoder.encode(e.getKey(),java.nio.charset.StandardCharsets.UTF_8)+"="+java.net.URLEncoder.encode(e.getValue(),java.nio.charset.StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        var mac=javax.crypto.Mac.getInstance("HmacSHA512");mac.init(new javax.crypto.spec.SecretKeySpec("local-normal-test-key".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA512"));String query=data+"&vnp_SecureHash="+HexFormat.of().formatHex(mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        for(int retry=0;retry<2;retry++)mvc.perform(get("/api/payments/vnpay/ipn?"+query)).andExpect(status().isOk());
        assertEquals("Paid",jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,invoice));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM normal_refunds f JOIN payment_resolutions r ON r.id=f.id WHERE r.invoice_id=?",Integer.class,invoice));
        assertEquals("Cancelled",jdbc.queryForObject("SELECT status FROM class_enrollments WHERE class_id=?",String.class,c));
    }
    @Test void staleClassVersionUsesConsistent409AndLeavesCommittedStateUntouched()throws Exception {
        int id=create(4,null).path("id").asInt();var session=login(manager);
        var before=List.of(jdbc.queryForList("SELECT * FROM classes WHERE class_id=?",id),jdbc.queryForList("SELECT * FROM sessions WHERE class_id=?",id),jdbc.queryForList("SELECT * FROM normal_proposals"),jdbc.queryForList("SELECT * FROM invoices"),jdbc.queryForList("SELECT * FROM system_logs"));
        for(int retry=0;retry<2;retry++){
            mvc.perform(post("/api/classes/"+id+"/normal-proposals").session(session).contentType("application/json").content(json.writeValueAsString(Map.of("kind","Postpone","shiftDays",7,"version",999,"reason","DEMO stale version"))))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.error").value("Conflict")).andExpect(jsonPath("$.message").value("NORMAL_VERSION_STALE")).andExpect(jsonPath("$.code").doesNotExist());
            assertEquals(before,List.of(jdbc.queryForList("SELECT * FROM classes WHERE class_id=?",id),jdbc.queryForList("SELECT * FROM sessions WHERE class_id=?",id),jdbc.queryForList("SELECT * FROM normal_proposals"),jdbc.queryForList("SELECT * FROM invoices"),jdbc.queryForList("SELECT * FROM system_logs")));
        }
    }
    @Test void staleTransferScheduleCannotApplyOldConsentButMemberStillCanChooseRefund()throws Exception {
        int source=create(4,null).path("id").asInt();enroll(source,member,"Cash");
        var payload=new HashMap<>(creation(1,null));var details=details(1,null);details.put("startDate",BASE.toLocalDate().plusDays(49).toString());payload.put("sportsClass",details);int target=postAs("/api/classes/normal-with-sessions",manager,payload,201).path("id").asInt();
        var p=postAs("/api/classes/"+source+"/normal-proposals",manager,Map.of("kind","Transfer","targetClassId",target,"version",0,"reason","Center changes original plan"),200);int proposal=p.path("id").asInt();
        jdbc.update("UPDATE invoices SET reconciliation_required=true WHERE class_id=?",source);
        assertEquals("NORMAL_RECEIPT_NOT_ELIGIBLE",postAs("/api/normal-proposals/"+proposal+"/respond",member,Map.of("version",1,"fingerprint",p.path("fingerprint").asText(),"choice","Accept"),409).path("code").asText());
        jdbc.update("UPDATE invoices SET reconciliation_required=false WHERE class_id=?",source);
        jdbc.update("UPDATE sessions SET session_date=session_date+7 WHERE class_id=?",target);
        postAs("/api/normal-proposals/"+proposal+"/respond",member,Map.of("version",1,"fingerprint",p.path("fingerprint").asText(),"choice","Accept"),409);
        postAs("/api/normal-proposals/"+proposal+"/respond",member,Map.of("version",1,"fingerprint",p.path("fingerprint").asText(),"choice","Refund"),200);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM normal_refunds WHERE class_id=?",Integer.class,source));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM normal_transfers WHERE proposal_id=?",Integer.class,proposal));
    }
}
