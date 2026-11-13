package com.sportscenter.room;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sportscenter.user.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** D02 Session1.3: project schema50/100; real HTTP/login and committed DB/audit readback. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.config.import=", "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:room_validation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false",
        "vnpay.query-enabled=false", "vnpay.query-settlement-enabled=false", "vnpay.network-probe-enabled=false", "scms.ai.chat.enabled=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomValidationIntegrationTest {
    @DynamicPropertySource static void disposablePostgres(DynamicPropertyRegistry r) {
        String url=System.getProperty("scms.room-validation.pg-url", ""); if (url.isBlank()) return;
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+")) throw new IllegalArgumentException("Disposable loopback DB only");
        r.add("spring.datasource.url",()->url); r.add("spring.datasource.username",()->"postgres"); r.add("spring.datasource.password",()->"scms-local-only");
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver"); r.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect"); r.add("spring.jpa.hibernate.ddl-auto",()->"none");
    }
    @LocalServerPort int port;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    @MockBean com.sportscenter.invoice.PaymentExpirationJob paymentJob;
    @MockBean com.sportscenter.membership.PackageExpirationJob packageJob;
    @MockBean com.sportscenter.training.AttendanceFinalizationJob attendanceJob;
    @MockBean com.sportscenter.notification.NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.sportclass.NormalLifecycleJob lifecycleJob;
    @MockBean com.sportscenter.auth.EmailService email;
    private User manager, managerB, coach;
    private Room existing;
    private int sequence;
    private static final String PASSWORD="Room-validation-local-2026!";

    private void tx(Runnable work) { new TransactionTemplate(transactions).executeWithoutResult(t->work.run()); }
    @BeforeAll void actors() {
        tx(()->{
            if (System.getProperty("scms.room-validation.pg-url", "").isBlank()) jdbc.update("INSERT INTO centers(center_id) VALUES(1),(2)");
            else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(1,'Room validation1'),(2,'Room validation2') ON CONFLICT(center_id) DO NOTHING");
            var permission=new Permission(); permission.setName("MANAGE_CLASSES"); em.persist(permission);
            var mr=new Role(); mr.setName("CenterManager"); mr.getPermissions().add(permission); em.persist(mr);
            var cr=new Role(); cr.setName("Coach"); cr.getPermissions().add(permission); em.persist(cr);
            manager=user("manager",mr,1); managerB=user("manager-b",mr,2); coach=user("coach",cr,1);
            var cp=new Coach(); cp.setUser(coach); em.persist(cp);
        });
    }
    private User user(String name,Role role,int center) {
        var u=new User(); u.setEmail(name+"@room-validation.test.invalid"); u.setFullName(name); u.setStatus("Active"); u.setCenterId(center); u.setRole(role); u.setPasswordHash(passwords.encode(PASSWORD)); em.persist(u); return u;
    }
    @BeforeEach void fixture() {
        sequence++;
        tx(()->{ existing=new Room(); existing.setName("Validation seed "+sequence); existing.setLocation("Original location"); existing.setCapacity(15); existing.setStatus("Available"); existing.setCenterId(1); existing.setActivityType(RoomActivity.BADMINTON); em.persist(existing); });
    }
    private HttpResponse<String> send(HttpClient client,String method,String path,Object body) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(30));
        if (body==null) b.GET(); else b.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    private HttpClient login(User actor) throws Exception {
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        assertEquals(200,send(client,"POST","/api/auth/login",Map.of("email",actor.getEmail(),"password",PASSWORD)).statusCode());
        var me=send(client,"GET","/api/auth/me",null); assertEquals(200,me.statusCode()); assertEquals(actor.getId(),json.readTree(me.body()).path("userId").asInt()); return client;
    }
    private Map<String,Object> body() {
        return new LinkedHashMap<>(Map.of("name","Validation court","location","Validation hall","capacity",15,"status","Available","facilityType","Indoor","activityType","BADMINTON"));
    }
    private HttpResponse<String> write(HttpClient client,boolean update,Map<String,Object> b) throws Exception {
        return send(client,update?"PUT":"POST","/api/rooms"+(update?"/"+existing.getId():""),b);
    }
    private List<Object> snapshot() {
        return List.of(jdbc.queryForList("SELECT * FROM rooms ORDER BY room_id"),jdbc.queryForList("SELECT * FROM system_logs ORDER BY log_id"),jdbc.queryForList("SELECT * FROM sessions ORDER BY session_id"));
    }
    private void invalid(HttpClient client,boolean update,Map<String,Object> b,String field) throws Exception {
        var before=snapshot(); var response=write(client,update,b); var error=json.readTree(response.body());
        assertAll(()->assertEquals(400,response.statusCode()),()->assertTrue(error.path("message").asText().startsWith(field+":"),"Bean Validation field message must precede DB handling"),()->assertEquals(before,snapshot(),"Rejected request must not persist room/schedule/audit changes"));
    }
    private void saved(HttpClient client,boolean update,Map<String,Object> b) throws Exception {
        var response=write(client,update,b); assertEquals(update?200:201,response.statusCode()); var row=json.readTree(response.body()); int id=row.path("id").asInt();
        assertEquals(b.get("name"),row.path("name").asText());
        assertEquals(b.get("location"),row.path("location").isNull()?null:row.path("location").asText());
        assertEquals(b.get("name"),jdbc.queryForObject("SELECT room_name FROM rooms WHERE room_id=?",String.class,id));
        assertEquals(b.get("location"),jdbc.queryForObject("SELECT location FROM rooms WHERE room_id=?",String.class,id));
        var read=send(client,"GET","/api/rooms",null); assertEquals(200,read.statusCode());
        JsonNode found=null; for (JsonNode candidate:json.readTree(read.body())) if (candidate.path("id").asInt()==id) found=candidate;
        assertNotNull(found); assertEquals(b.get("name"),found.path("name").asText());
        assertEquals(b.get("location"),found.path("location").isNull()?null:found.path("location").asText());
        assertEquals(manager.getId(),jdbc.queryForObject("SELECT user_id FROM system_logs WHERE target_entity='ROOM' AND target_id=? ORDER BY log_id DESC LIMIT 1",Integer.class,id));
    }
    @Test void databaseBoundsMatchProjectSchemaInsteadOfJpaDefaults() {
        String query="SELECT character_maximum_length FROM information_schema.columns WHERE lower(table_schema)=lower(current_schema()) AND lower(table_name)='rooms' AND lower(column_name)=?";
        assertEquals(50,jdbc.queryForObject(query,Integer.class,"room_name")); assertEquals(100,jdbc.queryForObject(query,Integer.class,"location"));
        String nullable="SELECT is_nullable FROM information_schema.columns WHERE lower(table_schema)=lower(current_schema()) AND lower(table_name)='rooms' AND lower(column_name)=?";
        assertEquals("NO",jdbc.queryForObject(nullable,String.class,"room_name")); assertEquals("YES",jdbc.queryForObject(nullable,String.class,"location"));
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void name51Is400BeforeDbAndDoesNotTrimOrTruncate(boolean update) throws Exception {
        var client=login(manager);
        for (String name:List.of("N".repeat(51)," "+"N".repeat(49)+" ")) { var b=body(); b.put("name",name); invalid(client,update,b,"name"); }
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void location101Is400BeforeDbWithoutMutation(boolean update) throws Exception {
        var client=login(manager); var b=body(); b.put("location","L".repeat(101)); invalid(client,update,b,"location");
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void exactName50AndLocation100PersistWithoutTruncation(boolean update) throws Exception {
        var client=login(manager); var b=body(); String suffix=Integer.toString(sequence);
        b.put("name","Đ".repeat(50-suffix.length())+suffix); b.put("location","K".repeat(100)); saved(client,update,b);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void nameIsStillRequiredAndNotBlank(boolean update) throws Exception {
        var client=login(manager);
        for (String value:Arrays.asList(null,"","   ")) { var b=body(); b.put("name",value); invalid(client,update,b,"name"); }
        var omitted=body(); omitted.remove("name"); invalid(client,update,omitted,"name");
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void locationNullOmittedEmptyAndWhitespaceRetainExistingStorageSemantics(boolean update) throws Exception {
        var client=login(manager); int variant=0;
        for (String location:Arrays.asList("  Hall  ","   ","",null)) { var b=body(); b.put("name","  Court "+sequence+"-"+(++variant)+"  "); b.put("location",location); saved(client,update,b); }
        var omitted=body(); omitted.put("name","  Omitted "+sequence+"  "); omitted.remove("location"); saved(client,update,omitted);
    }
    @Test void validRequestsStillRequireRoleAndCorrectCenter() throws Exception {
        var before=snapshot(); assertEquals(403,write(login(coach),false,body()).statusCode());
        assertEquals(403,write(login(managerB),true,body()).statusCode()); assertEquals(before,snapshot());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void duplicateNameIs409OnCreateAndUpdateWithRollbackAndRetry(boolean update) throws Exception {
        // H2 has no partial expression index from the PG migration; exact-name fixture only.
        if(System.getProperty("scms.room-validation.pg-url", "").isBlank())jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS test_room_exact_name ON rooms(center_id,room_name)");
        String duplicate="Duplicate court "+sequence;
        tx(()->{var r=new Room();r.setName(duplicate);r.setCenterId(1);r.setCapacity(15);r.setStatus("Available");r.setActivityType(RoomActivity.BADMINTON);em.persist(r);});
        var client=login(manager);var b=body();b.put("name",duplicate);var before=snapshot();
        for(int i=0;i<2;i++){var response=write(client,update,b);var error=json.readTree(response.body());assertEquals(409,response.statusCode(),response.body());assertEquals(409,error.path("status").asInt());assertEquals("Conflict",error.path("error").asText());assertEquals("Operation violates a database constraint",error.path("message").asText());assertEquals(before,snapshot());}
        b.put("name","Recovered court "+sequence);saved(client,update,b);
    }
    @Test void errorAndDeprecationMetadataMatchesScopedRuntimeContract() throws Exception {
        var response=send(HttpClient.newHttpClient(),"GET","/v3/api-docs",null);assertEquals(200,response.statusCode());var paths=json.readTree(response.body()).path("paths");
        var retired=Map.of("/api/packages","post","/api/packages/{id}","put","/api/packages/{id}/status","patch",
                "/api/members/{memberId}/packages","post","/api/members/{memberId}/subscriptions","post","/api/members/me/subscriptions","post","/api/member-packages/subscribe-me","post");
        retired.forEach((path,method)->{var op=paths.path(path).path(method);assertTrue(op.path("deprecated").asBoolean(),path);assertTrue(op.path("responses").has("400"),path);assertFalse(op.path("responses").has("201"),path);assertFalse(op.path("responses").has("200"),path);});
        for(String path:List.of("/api/member-packages/{id}/status","/api/subscriptions/{id}/status","/api/member-packages/{id}/extend","/api/subscriptions/{id}/extend")){
            var op=paths.path(path).path(path.endsWith("status")?"patch":"post");assertFalse(op.path("deprecated").asBoolean(),path);assertTrue(op.path("responses").has("400"),path);assertTrue(op.path("responses").has("404"),path);
        }
        for(var pair:List.of(Map.entry("/api/roles/{id}/permissions","put"),Map.entry("/api/training-results","post"),Map.entry("/api/training-results/bulk","post"),Map.entry("/api/normal-refunds/{id}/approve","post"))){
            var responses=paths.path(pair.getKey()).path(pair.getValue()).path("responses");for(String code:List.of("400","401","403","404"))assertTrue(responses.has(code),pair+" "+code);
        }
        assertTrue(paths.path("/api/normal-refunds/{id}/approve").path("post").path("responses").has("409"));
        assertFalse(paths.path("/api/advanced-registrations").path("post").path("deprecated").asBoolean());
    }
    @Test void openApiDeclaresRequestBoundsAndKeepsOptionalLocation() throws Exception {
        var response=send(HttpClient.newHttpClient(),"GET","/v3/api-docs",null); assertEquals(200,response.statusCode()); JsonNode schema=json.readTree(response.body());
        var request=schema.path("components").path("schemas").path("RoomRequest");
        assertEquals(50,request.path("properties").path("name").path("maxLength").asInt()); assertEquals(100,request.path("properties").path("location").path("maxLength").asInt());
        var required=new HashSet<String>(); request.path("required").forEach(x->required.add(x.asText())); assertTrue(required.contains("name")); assertFalse(required.contains("location"));
        String output=System.getProperty("scms.room-validation.openapi", "");
        if (!output.isBlank()) {
            ((ObjectNode)schema).putArray("servers").addObject().put("url","http://localhost").put("description","Generated server url");
            Path path=Path.of(output); Files.createDirectories(path.toAbsolutePath().getParent()); Files.writeString(path,json.writerWithDefaultPrettyPrinter().writeValueAsString(schema));
        }
    }
}
