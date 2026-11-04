package com.sportscenter.invoice;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import com.sportscenter.membership.*;
import com.sportscenter.user.*;
import com.sportscenter.user.Role;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Authenticated local HTTP plus committed state. Provider is a local protocol simulator, not VNPay. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "spring.config.import=", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.url=jdbc:h2:mem:querydr;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never", "spring.jpa.open-in-view=false",
        "vnpay.tmn-code=TEST0001", "vnpay.hash-secret=test-query-secret", "payment.expire-minutes=5",
        "vnpay.query-enabled=true", "vnpay.query-settlement-enabled=true", "vnpay.query-ip-address=127.0.0.1",
        "logging.level.root=WARN", "debug=false"})
@Import(VnPayQueryIntegrationTest.Config.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VnPayQueryIntegrationTest {
    static final ObjectMapper JSON=new ObjectMapper();
    static final AtomicInteger providerCalls=new AtomicInteger();
    static final HttpServer PROVIDER=provider();
    static volatile String mode="success";
    static volatile CountDownLatch entered,release;
    static HttpServer provider() {
        try {
            HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.setExecutor(Executors.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t;}));
            server.createContext("/query",exchange->{
                try {
                    providerCalls.incrementAndGet();
                    JsonNode req=JSON.readTree(exchange.getRequestBody());
                    assertEquals("querydr",req.path("vnp_Command").asText());
                    if("block".equals(mode)){entered.countDown();assertTrue(release.await(8,TimeUnit.SECONDS));}
                    if("timeout".equals(mode))Thread.sleep(5500);
                    if("http-error".equals(mode)){exchange.sendResponseHeaders(503,-1);return;}
                    String ref=req.path("vnp_TxnRef").asText();
                    var p=VnPayQueryClientTest.response(ref,"90000000",transaction(ref));
                    if("older-paid".equals(mode)&&!"20261104110000".equals(req.path("vnp_TransactionDate").asText()))p.put("vnp_TransactionStatus","01");
                    if("pending".equals(mode))p.put("vnp_TransactionStatus","01");
                    if("amount".equals(mode))p.put("vnp_Amount","89999999");
                    if("merchant".equals(mode))p.put("vnp_TmnCode","OTHER001");
                    if("reference".equals(mode))p.put("vnp_TxnRef","OTHER-ORDER");
                    VnPayQueryClientTest.signed(p);
                    if("signature".equals(mode))p.put("vnp_SecureHash","0".repeat(128));
                    if("unsigned".equals(mode))p.remove("vnp_SecureHash");
                    byte[] body=JSON.writeValueAsBytes(p);exchange.sendResponseHeaders(200,body.length);
                    exchange.getResponseBody().write(body);
                } catch(Exception ex){exchange.sendResponseHeaders(500,-1);} finally{exchange.close();}
            });server.start();return server;
        } catch(Exception ex){throw new ExceptionInInitializerError(ex);}
    }
    static String transaction(String ref){return "8"+ref.substring(ref.lastIndexOf('-')+1);}
    public static class MutableClock extends Clock {
        volatile Instant now=Instant.parse("2026-11-04T04:00:00Z");
        public ZoneId getZone(){return ZoneId.of("Asia/Ho_Chi_Minh");}
        public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}
        public Instant instant(){return now;}
    }
    @TestConfiguration static class Config {@Bean @Primary MutableClock queryClock(){return new MutableClock();}}
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){
        r.add("vnpay.query-url",()->"http://127.0.0.1:"+PROVIDER.getAddress().getPort()+"/query");
        String url=System.getProperty("scms.query.pg-url","");
        if(!url.isBlank()) {
            if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_querydr_[a-z0-9_]+"))throw new IllegalArgumentException("Disposable local QueryDR DB only");
            r.add("spring.datasource.url",()->url);r.add("spring.datasource.username",()->"postgres");r.add("spring.datasource.password",()->"scms-local-only");
            r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");r.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect");
            r.add("spring.jpa.hibernate.ddl-auto",()->"none");
        }
    }
    @MockBean PaymentExpirationJob paymentJob;
    @MockBean PackageExpirationJob packageJob;
    @MockBean com.sportscenter.training.AttendanceFinalizationJob attendanceJob;
    @MockBean com.sportscenter.notification.NotificationScheduleJob notificationJob;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired MemberRepository members;
    @Autowired MembershipPackageRepository packages;
    @Autowired MemberPackageRepository subscriptions;
    @Autowired InvoiceRepository invoices;
    @Autowired VnPayPaymentAttemptRepository attempts;
    @Autowired InvoiceService service;
    @Autowired PasswordEncoder passwords;
    @Autowired PaymentProperties config;
    @Autowired MutableClock clock;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
    @LocalServerPort int port;
    int memberId,otherId,staffId;
    HttpClient member,other,staff;
    record Order(int id,int subscription,String code){}

    @BeforeAll void setup() throws Exception {
        tx.executeWithoutResult(t->{
            jdbc.execute("CREATE SEQUENCE IF NOT EXISTS invoice_code_seq START WITH 10000");
            boolean namedCenters=jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE lower(table_name)='centers' AND lower(column_name)='center_name'",Integer.class)>0;
            for(int center:List.of(1,2))if(jdbc.queryForObject("SELECT count(*) FROM centers WHERE center_id=?",Integer.class,center)==0){
                if(namedCenters)jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(?,?)",center,"Query test center "+center);
                else jdbc.update("INSERT INTO centers(center_id) VALUES(?)",center);
            }
            Role mr=role("Member"),sr=role("CenterManager");
            memberId=user("query.member",mr).getId();otherId=user("query.other",mr).getId();staffId=user("query.staff",sr).getId();
            for(int id:List.of(memberId,otherId)){var m=new Member();m.setUser(users.findById(id).orElseThrow());m.setJoinDate(LocalDate.now(clock));members.save(m);}
        });
        member=login("query.member");other=login("query.other");staff=login("query.staff");
    }
    Role role(String name){return roles.findAll().stream().filter(r->name.equals(r.getName())).findFirst().orElseGet(()->{var r=new Role();r.setName(name);return roles.save(r);});}
    User user(String name,Role role){var u=new User();u.setFullName(name);u.setEmail(name+"@example.test");u.setRole(role);u.setStatus("Active");u.setCenterId(1);u.setPasswordHash(passwords.encode("LocalTest123!"));return users.save(u);}
    HttpClient login(String name)throws Exception{
        var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        call(c,"POST","/api/auth/login",Map.of("email",name+"@example.test","password","LocalTest123!"),200);
        assertEquals(name+"@example.test",call(c,"GET","/api/auth/me",null,200).path("email").asText());return c;
    }
    @BeforeEach void reset()throws Exception{
        mode="success";clock.now=Instant.parse("2026-11-04T04:00:00Z");
        ReflectionTestUtils.setField(config,"vnpayQueryEnabled",true);ReflectionTestUtils.setField(config,"vnpayQuerySettlementEnabled",true);
        for(HttpClient c:List.of(member,other,staff))call(c,"GET","/api/auth/me",null,200);
    }
    JsonNode call(HttpClient c,String method,String path,Object body,int status)throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20))
                .header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))).build();
        var response=c.send(request,HttpResponse.BodyHandlers.ofString());
        assertEquals(status,response.statusCode(),method+" "+path+" "+response.body());
        return response.body().isBlank()?JSON.createObjectNode():JSON.readTree(response.body());
    }
    JsonNode verify(Order o)throws Exception{return call(member,"POST","/api/members/me/invoices/"+o.id+"/verify-vnpay",Map.of(),200);}
    Order order(){return tx.execute(t->{
        MembershipPackage p=new MembershipPackage();p.setName("Query fixture "+UUID.randomUUID());p.setPrice(new BigDecimal("900000"));p.setDurationDays(30);p.setStatus("Active");p.setCenterId(1);packages.save(p);
        MemberPackage s=new MemberPackage();s.setMember(users.findById(memberId).orElseThrow());s.setMembershipPackage(p);s.setStatus("Pending");subscriptions.save(s);
        var o=service.createPackageOrder(s.getMember(),p,"VNPay",s,"127.0.0.1");return new Order(o.invoiceId(),s.getSubscriptionId(),o.invoiceCode());
    });}
    Order advanced(){return tx.execute(t->{
        User coach=user("query.coach."+UUID.randomUUID(),role("Coach"));
        var profile=new Coach();profile.setUser(coach);em.persist(profile);
        var subject=em.createQuery("select s from Subject s where s.slug=:slug",com.sportscenter.subject.Subject.class).setParameter("slug","cau-long").getResultStream().findFirst().orElseGet(()->{
            var s=new com.sportscenter.subject.Subject();s.setName("Query badminton");s.setSlug("cau-long");s.setStatus("Active");s.setOutdoor(false);s.setNormalPrice(new BigDecimal("900000"));s.setAdvancedPrice(new BigDecimal("900000"));em.persist(s);return s;
        });
        var spec=new com.sportscenter.specialization.Specialization();spec.setName("Query subject "+UUID.randomUUID());spec.setSubject(subject);spec.setStatus("Active");em.persist(spec);
        em.persist(new com.sportscenter.specialization.CoachSpecialization(profile,spec));
        var schedule=new CoachWorkSchedule();schedule.setCoach(coach);schedule.setAcceptingNew(true);
        for(DayOfWeek day:DayOfWeek.values())for(int slot=1;slot<=7;slot++)schedule.getWeeklySlots().add(day.name()+":"+slot);em.persist(schedule);
        var room=new com.sportscenter.room.Room();room.setName("Query room "+UUID.randomUUID());room.setCapacity(15);room.setCenterId(1);room.setStatus("Available");room.setFacilityType("Indoor");em.persist(room);
        var s=new MemberPackage();s.setMember(users.findById(memberId).orElseThrow());s.setSubject(subject);s.setPrimaryCoach(coach);s.setPurchaseAmount(new BigDecimal("900000"));s.setStatus("Pending");subscriptions.save(s);
        var o=service.createAdvancedOrder(s,"VNPay","127.0.0.1");return new Order(o.invoiceId(),s.getSubscriptionId(),o.invoiceCode());
    });}
    void state(Order o,String payment,String sub,int credit){
        assertEquals(payment,jdbc.queryForObject("SELECT payment_status FROM invoices WHERE invoice_id=?",String.class,o.id));
        assertEquals(sub,jdbc.queryForObject("SELECT status FROM member_packages WHERE subscription_id=?",String.class,o.subscription));
        assertEquals(credit,jdbc.queryForObject("SELECT remaining_credit FROM member_packages WHERE subscription_id=?",Integer.class,o.subscription));
    }

    @Test void verifiedResponseWritesCommittedPaymentAndRetryDoesNotRepeatActivation()throws Exception{
        Order o=order();int calls=providerCalls.get();
        var r=verify(o);assertEquals("VERIFIED_PAID",r.path("verificationStatus").asText());
        state(o,"Paid","Active",0);assertFalse(r.path("invoice").path("reconciliationRequired").asBoolean());
        assertEquals(transaction(o.code),jdbc.queryForObject("SELECT gateway_transaction_ref FROM invoices WHERE invoice_id=?",String.class,o.id));
        assertNotNull(jdbc.queryForObject("SELECT payment_date FROM invoices WHERE invoice_id=?",java.sql.Timestamp.class,o.id));
        var start=jdbc.queryForObject("SELECT start_date FROM member_packages WHERE subscription_id=?",java.sql.Date.class,o.subscription);
        assertEquals("ALREADY_PAID",verify(o).path("verificationStatus").asText());
        assertEquals(start,jdbc.queryForObject("SELECT start_date FROM member_packages WHERE subscription_id=?",java.sql.Date.class,o.subscription));
        assertEquals(calls+1,providerCalls.get());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action='VERIFY_VNPAY_QUERY' AND target_id=?",Integer.class,o.id));
    }
    @Test void failureEvidenceNeverWritesPaidAndCooldownLimitsRetry()throws Exception{
        for(String value:List.of("signature","unsigned","amount","merchant","reference","http-error","timeout","pending")){
            Order o=order();mode=value;int before=providerCalls.get();var r=verify(o);
            assertEquals("pending".equals(value)?"NOT_CONFIRMED":"QUERY_UNAVAILABLE",r.path("verificationStatus").asText());
            state(o,"Pending","Pending",0);
            assertNull(jdbc.queryForObject("SELECT gateway_transaction_ref FROM invoices WHERE invoice_id=?",String.class,o.id));
            assertEquals("RETRY_LATER",verify(o).path("verificationStatus").asText());assertEquals(before+1,providerCalls.get());
            clock.now=clock.now.plusSeconds(31);mode="success";assertEquals("VERIFIED_PAID",verify(o).path("verificationStatus").asText());
        }
    }
    @Test void atDeadlineRecordsMoneyForReconciliationWithoutRevivingEntitlement()throws Exception{
        Order o=order();clock.now=clock.now.plusSeconds(300);service.expireInvoice(o.id);
        var r=verify(o);assertEquals("VERIFIED_PAID",r.path("verificationStatus").asText());
        assertTrue(r.path("invoice").path("reconciliationRequired").asBoolean());state(o,"Paid","Cancelled",0);
    }
    @Test void beforeDeadlineActivatesAndUnavailablePackageRequiresReconciliation()throws Exception{
        Order o=order();clock.now=clock.now.plusSeconds(299);assertEquals("VERIFIED_PAID",verify(o).path("verificationStatus").asText());state(o,"Paid","Active",0);
        Order unavailable=order();jdbc.update("UPDATE membership_packages SET status='Inactive' WHERE package_id=(SELECT package_id FROM invoices WHERE invoice_id=?)",unavailable.id);
        assertTrue(verify(unavailable).path("invoice").path("reconciliationRequired").asBoolean());state(unavailable,"Paid","Cancelled",0);
    }
    @Test void originalLinkMetadataIsExactDeduplicatedAndReadsDoNotQuery()throws Exception{
        Order o=order();int before=providerCalls.get();
        call(member,"GET","/api/members/me/invoices",null,200);assertEquals(before,providerCalls.get());
        String date=jdbc.queryForObject("SELECT transaction_date FROM vnpay_payment_attempts WHERE invoice_id=?",String.class,o.id);
        assertEquals("20261104110000",date);
        call(member,"POST","/api/invoices/"+o.id+"/pay-online",Map.of(),200);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM vnpay_payment_attempts WHERE invoice_id=?",Integer.class,o.id));
        clock.now=clock.now.plusSeconds(20);
        var link=call(member,"POST","/api/invoices/"+o.id+"/pay-online",Map.of(),200).path("paymentUrl").asText();
        assertTrue(link.contains("vnp_CreateDate=20261104110020"));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM vnpay_payment_attempts WHERE invoice_id=?",Integer.class,o.id));
        assertEquals(90000000L,jdbc.queryForObject("SELECT min(amount_minor) FROM vnpay_payment_attempts WHERE invoice_id=?",Long.class,o.id));
        assertEquals(before,providerCalls.get());
    }
    @Test void olderPaidLinkIsStillCheckedAndCatalogPriceCannotChangeEvidence()throws Exception{
        Order o=order();clock.now=clock.now.plusSeconds(20);
        call(member,"POST","/api/invoices/"+o.id+"/pay-online",Map.of(),200);
        jdbc.update("UPDATE membership_packages SET price=990000 WHERE package_id=(SELECT package_id FROM invoices WHERE invoice_id=?)",o.id);
        mode="older-paid";int before=providerCalls.get();assertEquals("VERIFIED_PAID",verify(o).path("verificationStatus").asText());
        assertEquals(before+2,providerCalls.get());state(o,"Paid","Active",0);
        assertEquals(0,new BigDecimal("900000").compareTo(jdbc.queryForObject("SELECT purchase_amount FROM member_packages WHERE subscription_id=?",BigDecimal.class,o.subscription)));
    }
    @Test void queryAloneGrantsTwelveCreditsOnceAndLaterIpnIsIdempotent()throws Exception{
        Order o=advanced();assertEquals("VERIFIED_PAID",verify(o).path("verificationStatus").asText());state(o,"Paid","Active",12);
        assertEquals("ALREADY_PAID",verify(o).path("verificationStatus").asText());
        assertEquals("02",call(HttpClient.newHttpClient(),"GET",ipn(o),null,200).path("RspCode").asText());state(o,"Paid","Active",12);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notifications WHERE dedupe_key=?",Integer.class,"PAYMENT_SUCCESS:"+o.id));
    }
    @Test void ownerRoleCenterActiveAndBodyAreEnforcedBeforeProviderCall()throws Exception{
        Order o=order();String path="/api/members/me/invoices/"+o.id+"/verify-vnpay";int before=providerCalls.get();
        call(HttpClient.newHttpClient(),"POST",path,Map.of(),401);call(other,"POST",path,Map.of(),404);call(staff,"POST",path,Map.of(),403);
        call(member,"POST",path,Map.of("amount",1,"status","00"),400);
        // Invoice center is derived from its owner. Use a distinct owner in center2.
        Order foreign=order();jdbc.update("UPDATE users SET center_id=2 WHERE user_id=?",otherId);
        jdbc.update("UPDATE invoices SET member_id=? WHERE invoice_id=?",otherId,foreign.id);
        try{call(member,"POST","/api/members/me/invoices/"+foreign.id+"/verify-vnpay",Map.of(),404);}
        finally{jdbc.update("UPDATE users SET center_id=1 WHERE user_id=?",otherId);}
        try{jdbc.update("UPDATE users SET status='Inactive' WHERE user_id=?",memberId);call(member,"POST",path,Map.of(),403);}
        finally{jdbc.update("UPDATE users SET status='Active' WHERE user_id=?",memberId);}
        assertEquals(before,providerCalls.get());state(o,"Pending","Pending",0);
    }
    @Test void missingMetadataDisabledAndReadOnlyModesNeverSetPaid()throws Exception{
        Order old=order();jdbc.update("DELETE FROM vnpay_payment_attempts WHERE invoice_id=?",old.id);int before=providerCalls.get();
        assertEquals("ATTEMPT_MISSING",verify(old).path("verificationStatus").asText());
        Order disabled=order();ReflectionTestUtils.setField(config,"vnpayQueryEnabled",false);
        assertEquals("DISABLED",verify(disabled).path("verificationStatus").asText());assertEquals(before,providerCalls.get());
        ReflectionTestUtils.setField(config,"vnpayQueryEnabled",true);ReflectionTestUtils.setField(config,"vnpayQuerySettlementEnabled",false);
        assertEquals("VERIFIED_ONLY",verify(disabled).path("verificationStatus").asText());state(disabled,"Pending","Pending",0);
    }
    @Test void ipnCanCommitWhileProviderWaitsAndQueryRetryCannotActivateTwice()throws Exception{
        Order o=advanced();mode="block";entered=new CountDownLatch(1);release=new CountDownLatch(1);
        var pending=CompletableFuture.supplyAsync(()->{try{return verify(o);}catch(Exception e){throw new CompletionException(e);}});
        try{
            assertTrue(entered.await(5,TimeUnit.SECONDS));assertEquals("RETRY_LATER",verify(o).path("verificationStatus").asText());
            assertEquals("00",call(HttpClient.newHttpClient(),"GET",ipn(o),null,200).path("RspCode").asText());
            state(o,"Paid","Active",12);
        }finally{release.countDown();}
        assertEquals("ALREADY_PAID",pending.get(8,TimeUnit.SECONDS).path("verificationStatus").asText());
        assertEquals("02",call(HttpClient.newHttpClient(),"GET",ipn(o),null,200).path("RspCode").asText());state(o,"Paid","Active",12);
        assertNotNull(jdbc.queryForObject("SELECT expires_at FROM member_packages WHERE subscription_id=?",java.sql.Timestamp.class,o.subscription));
    }
    String ipn(Order o)throws Exception{
        var p=new TreeMap<String,String>();p.put("vnp_TmnCode","TEST0001");p.put("vnp_TxnRef",o.code);p.put("vnp_Amount","90000000");
        p.put("vnp_ResponseCode","00");p.put("vnp_TransactionStatus","00");p.put("vnp_TransactionNo",transaction(o.code));
        String data=String.join("&",p.entrySet().stream().map(e->e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.US_ASCII)).toList());
        return "/api/payments/vnpay/ipn?"+data+"&vnp_SecureHash="+VnPayQueryClientTest.hmac(data);
    }
    @Test void duplicateTransactionCannotBeAttachedToAnotherInvoice()throws Exception{
        Order o=order(),used=order();
        jdbc.update("UPDATE invoices SET gateway_transaction_ref=? WHERE invoice_id=?",transaction(o.code),used.id);
        call(member,"POST","/api/members/me/invoices/"+o.id+"/verify-vnpay",Map.of(),409);state(o,"Pending","Pending",0);
    }
    @AfterAll void snapshot()throws Exception{
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/v3/api-docs")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());var spec=JSON.readTree(response.body());
        var endpoint=spec.path("paths").path("/api/members/me/invoices/{id}/verify-vnpay").path("post");
        assertFalse(endpoint.isMissingNode());
        if(Boolean.getBoolean("scms.query.export-openapi")) {
            ((com.fasterxml.jackson.databind.node.ObjectNode)spec).set("servers",JSON.valueToTree(List.of(Map.of("url","http://localhost:8080","description","Local API; choose deployed base URL when integrating"))));
            Files.writeString(Path.of("artifacts/fe-handoff/openapi.json"),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(spec)+"\n");
        }
        PROVIDER.stop(0);
    }
}
