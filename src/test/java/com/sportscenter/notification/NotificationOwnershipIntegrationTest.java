package com.sportscenter.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.user.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** In-app local fixtures only: real cookies/auth-me and committed owner/read-state checks. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.config.import=", "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:notification_ownership;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false",
        "vnpay.query-enabled=false", "vnpay.query-settlement-enabled=false", "vnpay.network-probe-enabled=false", "scms.ai.chat.enabled=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationOwnershipIntegrationTest {
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry r){
        String url=System.getProperty("scms.notification.pg-url","");if(url.isBlank())return;
        if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+"))throw new IllegalArgumentException("Disposable loopback DB only");
        r.add("spring.datasource.url",()->url);r.add("spring.datasource.username",()->"postgres");r.add("spring.datasource.password",()->"scms-local-only");r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");r.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect");r.add("spring.jpa.hibernate.ddl-auto",()->"none");
    }
    @LocalServerPort int port;
    @Autowired EntityManager em; @Autowired JdbcTemplate jdbc; @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper json; @Autowired PasswordEncoder passwords; @Autowired NotificationService service; @Autowired Clock clock;
    @Autowired ServletWebServerApplicationContext context;
    @MockBean com.sportscenter.invoice.PaymentExpirationJob paymentJob;
    @MockBean com.sportscenter.membership.PackageExpirationJob packageJob;
    @MockBean com.sportscenter.training.AttendanceFinalizationJob attendanceJob;
    @MockBean NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.sportclass.NormalLifecycleJob lifecycleJob;
    @MockBean com.sportscenter.auth.EmailService email;
    final Map<String,Role> roles=new LinkedHashMap<>();final Map<String,User> actors=new LinkedHashMap<>();final Map<String,Integer> direct=new LinkedHashMap<>();
    NotificationEvent event;int shared,sequence;
    static final String PASSWORD="Local-notification-only-2026!";
    void tx(Runnable r){new TransactionTemplate(transactions).executeWithoutResult(t->r.run());}
    @BeforeAll void roles(){tx(()->{
        if(System.getProperty("scms.notification.pg-url","").isBlank()){
            jdbc.update("INSERT INTO centers(center_id) VALUES(1)");
            // Match the existing PG dedupe constraint absent from JPA-generated H2 DDL.
            jdbc.execute("CREATE UNIQUE INDEX test_notification_dedupe ON notifications(dedupe_key)");
        }
        else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(1,'Notification DEMO') ON CONFLICT(center_id) DO NOTHING");
        for(String name:List.of("Admin","CenterManager","Receptionist","Coach","Member")){
            var role=new Role();role.setName(name);em.persist(role);roles.put(name,role);
            if(name.equals("Admin"))for(String authority:List.of("MANAGE_USERS","MANAGE_PERMISSIONS")){var p=new Permission();p.setName(authority);em.persist(p);role.getPermissions().add(p);}
        }
    });}
    @BeforeEach void fixtures(){sequence++;actors.clear();direct.clear();tx(()->{
        for(String name:List.of("Admin","CenterManager","Receptionist","Coach","Member","MemberB")){
            var u=new User();u.setFullName("DEMO "+name);u.setEmail(name.toLowerCase(Locale.ROOT)+sequence+"@notification.test.invalid");u.setRole(em.find(Role.class,roles.get(name.equals("MemberB")?"Member":name).getId()));u.setStatus("Active");u.setCenterId(1);u.setPasswordHash(passwords.encode(PASSWORD));em.persist(u);actors.put(name,u);
            for(int i=0;i<2;i++){var n=new Notification();n.setUser(u);n.setTitle("DEMO private "+name);n.setContent("DEMO owner message");n.setNotificationType("PAYMENT_SUCCESS");n.setType("Payment");n.setCreatedAt(LocalDateTime.now(clock));em.persist(n);if(i==0)direct.put(name,n.getId());}
        }
    });
        event=new NotificationEvent(actors.values().stream().map(User::getId).toList(),NotificationType.ENROLLMENT_CONFIRMED,"DEMO shared event","DEMO in-app only",null,null,"S33-NOTIFICATION-"+sequence);
        assertTrue(service.create(event));shared=jdbc.queryForObject("SELECT notification_id FROM notifications WHERE dedupe_key=?",Integer.class,event.dedupeKey());
    }
    HttpResponse<String> send(HttpClient c,String method,String path)throws Exception{return c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20)).method(method,HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());}
    HttpClient login(String role)throws Exception{
        var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        var body=json.writeValueAsString(Map.of("email",actors.get(role).getEmail(),"password",PASSWORD));var response=c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/auth/login")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode());
        var me=send(c,"GET","/api/auth/me");assertEquals(200,me.statusCode());assertEquals(actors.get(role).getId(),json.readTree(me.body()).path("userId").asInt());return c;
    }
    long unread(HttpClient c)throws Exception{var r=send(c,"GET","/api/notifications/me/unread-count");assertEquals(200,r.statusCode());return json.readTree(r.body()).path("unreadCount").asLong();}
    Object snapshot(){return List.of(jdbc.queryForList("SELECT * FROM notifications ORDER BY notification_id"),jdbc.queryForList("SELECT * FROM notification_recipients ORDER BY notification_id,user_id"),jdbc.queryForList("SELECT * FROM system_logs ORDER BY log_id"));}
    @ParameterizedTest @ValueSource(strings={"Admin","CenterManager","Receptionist","Coach","Member"})
    void eachRoleReadsOnlyOwnAndMarksOnlyOwnDirectAndRecipientState(String role)throws Exception{
        var c=login(role);assertEquals(3,unread(c));var before=snapshot();
        var page=send(c,"GET","/api/notifications/me?size=10&userId="+actors.get("MemberB").getId());assertEquals(200,page.statusCode());var rows=json.readTree(page.body());assertEquals(3,rows.path("totalElements").asInt());
        Set<Integer> ids=new HashSet<>();rows.path("content").forEach(n->ids.add(n.path("id").asInt()));assertTrue(ids.contains(shared));assertTrue(ids.contains(direct.get(role)));assertFalse(ids.contains(direct.get("MemberB")));assertEquals(before,snapshot());
        var denied=send(c,"PATCH","/api/notifications/"+direct.get("MemberB")+"/read");assertEquals(404,denied.statusCode());assertEquals("NOTIFICATION_NOT_FOUND",json.readTree(denied.body()).path("code").asText());assertEquals(before,snapshot());
        for(int retry=0;retry<2;retry++){var r=send(c,"PATCH","/api/notifications/"+direct.get(role)+"/read");assertEquals(204,r.statusCode());assertTrue(r.body().isEmpty());}
        assertTrue(jdbc.queryForObject("SELECT is_read FROM notifications WHERE notification_id=?",Boolean.class,direct.get(role)));assertEquals(2,unread(c));
        assertEquals(204,send(c,"PATCH","/api/notifications/"+shared+"/read").statusCode());assertEquals(1,unread(c));assertTrue(jdbc.queryForObject("SELECT is_read FROM notification_recipients WHERE notification_id=? AND user_id=?",Boolean.class,shared,actors.get(role).getId()));
        assertFalse(jdbc.queryForObject("SELECT is_read FROM notification_recipients WHERE notification_id=? AND user_id=?",Boolean.class,shared,actors.get("MemberB").getId()));
        assertEquals(3,unread(login("MemberB")));
        var filtered=send(c,"GET","/api/notifications/me?unreadOnly=true&type=PAYMENT_SUCCESS&size=1");assertEquals(1,json.readTree(filtered.body()).path("totalElements").asInt());assertEquals(1,json.readTree(filtered.body()).path("content").size());
    }
    @Test void markAllRetryStaysOwnAndEventDedupeDoesNotMultiplyRecipients()throws Exception{
        var c=login("Member");assertFalse(service.create(event));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE dedupe_key=?",Integer.class,event.dedupeKey()));assertEquals(6,jdbc.queryForObject("SELECT COUNT(*) FROM notification_recipients WHERE notification_id=?",Integer.class,shared));
        for(int i=0;i<2;i++)assertEquals(204,send(c,"PATCH","/api/notifications/me/read-all").statusCode());assertEquals(0,unread(c));assertEquals(3,unread(login("MemberB")));
    }
    @Test void anonymousGateCannotReadOrMarkAnyNotification()throws Exception{
        var before=snapshot();var c=HttpClient.newHttpClient();for(String p:List.of("/api/notifications/me","/api/notifications/me/unread-count"))assertEquals(401,send(c,"GET",p).statusCode());assertEquals(401,send(c,"PATCH","/api/notifications/"+shared+"/read").statusCode());assertEquals(before,snapshot());
    }
    @Test void actualContainerSessionTimeoutRequiresNewLoginWithoutChangingBusinessClock()throws Exception{
        var c=login("Member");var cookies=(CookieManager)c.cookieHandler().orElseThrow();String id=cookies.getCookieStore().getCookies().stream().filter(k->k.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
        var tomcat=((TomcatWebServer)context.getWebServer()).getTomcat();org.apache.catalina.Session session=null;
        for(var child:tomcat.getHost().findChildren())if(child instanceof org.apache.catalina.Context ctx){var found=ctx.getManager().findSession(id);if(found!=null)session=found;}
        assertNotNull(session);session.setMaxInactiveInterval(1);Thread.sleep(1500);var before=snapshot();assertEquals(401,send(c,"GET","/api/auth/me").statusCode());assertEquals(before,snapshot());assertEquals(3,unread(login("Member")));
    }
}
