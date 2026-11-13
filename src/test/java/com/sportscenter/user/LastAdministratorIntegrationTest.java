package com.sportscenter.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.sql.Connection;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** D01: isolated DEMO actors, real HTTP login/auth-me, independent committed DB/audit readback. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "spring.config.import=", "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:last_administrator;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false",
        "vnpay.query-enabled=false", "vnpay.query-settlement-enabled=false", "vnpay.network-probe-enabled=false", "scms.ai.chat.enabled=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LastAdministratorIntegrationTest {
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry r) {
        String url=System.getProperty("scms.last-admin.pg-url", ""); if (url.isBlank()) return;
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
    @Autowired AdministrationGuard guard;
    @MockBean com.sportscenter.invoice.PaymentExpirationJob paymentJob;
    @MockBean com.sportscenter.membership.PackageExpirationJob packageJob;
    @MockBean com.sportscenter.training.AttendanceFinalizationJob attendanceJob;
    @MockBean com.sportscenter.notification.NotificationScheduleJob notificationJob;
    @MockBean com.sportscenter.sportclass.NormalLifecycleJob lifecycleJob;
    @MockBean com.sportscenter.auth.EmailService email;
    private final Map<String,User> actors=new LinkedHashMap<>();
    private Role main, spare, partial, noMp, ordinary;
    private Permission mu,mp,mr;
    private static final String PASSWORD="Last-admin-local-2026!";
    private final List<Map<String,Object>> evidence=Collections.synchronizedList(new ArrayList<>());

    void tx(Runnable work) { new TransactionTemplate(transactions).executeWithoutResult(t->work.run()); }
    Permission permission(String name) { var p=new Permission();p.setName(name);em.persist(p);return p; }
    Role role(String name,Permission... ps) {var r=new Role();r.setName(name);r.getPermissions().addAll(List.of(ps));em.persist(r);return r;}
    User user(String name,Role r) { var u=new User();u.setEmail(name+"@last-admin.test.invalid");u.setFullName(name);u.setStatus("Active");u.setCenterId(1);u.setRole(r);u.setPasswordHash(passwords.encode(PASSWORD));em.persist(u);actors.put(name,u);return u; }
    @BeforeAll void fixtures() {
        tx(()->{
            if (System.getProperty("scms.last-admin.pg-url", "").isBlank()) jdbc.update("INSERT INTO centers(center_id) VALUES(1)");
            else jdbc.update("INSERT INTO centers(center_id,center_name) VALUES(1,'Last admin DEMO') ON CONFLICT(center_id) DO NOTHING");
            mu=permission("MANAGE_USERS");mp=permission("MANAGE_PERMISSIONS");mr=permission("MANAGE_RBAC");
            main=role("Admin",mu,mp,mr);spare=role("ADMIN",mu,mp,mr);partial=role("aDmIn",mp);noMp=role("adMIn",mu);ordinary=role("Ordinary",mu);
            user("admin-a",main);user("admin-b",main);user("spare",spare);user("partial",partial);user("no-mp",noMp);
            for(String name:List.of("CenterManager","Receptionist","Coach","Member")) user(name,role(name,mu,mp,mr));
        });
    }
    @BeforeEach void reset() {
        tx(()->{
            for(var u:actors.values()) { var managed=em.find(User.class,u.getId());managed.setRole(em.find(Role.class,u.getRole().getId()));managed.setStatus("Active");managed.setFullName(u.getFullName()); }
            em.find(User.class,actors.get("spare").getId()).setStatus("Inactive");
            em.find(Role.class,main.getId()).setPermissions(new HashSet<>(List.of(em.find(Permission.class,mu.getId()),em.find(Permission.class,mp.getId()),em.find(Permission.class,mr.getId()))));
            em.find(Role.class,spare.getId()).setPermissions(new HashSet<>(List.of(em.find(Permission.class,mu.getId()),em.find(Permission.class,mp.getId()),em.find(Permission.class,mr.getId()))));
        });
    }
    HttpResponse<String> send(HttpClient client,String method,String path,Object body) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(35));
        if(body==null)b.method(method,HttpRequest.BodyPublishers.noBody());else b.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response=client.send(b.build(),HttpResponse.BodyHandlers.ofString());
        if(!path.equals("/api/auth/login")) evidence.add(Map.of("method",method,"path",path,"http",response.statusCode(),"body",response.body()));
        return response;
    }
    HttpClient login(String name) throws Exception {
        var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(10)).build();
        assertEquals(200,send(c,"POST","/api/auth/login",Map.of("email",actors.get(name).getEmail(),"password",PASSWORD)).statusCode());
        var me=send(c,"GET","/api/auth/me",null);assertEquals(200,me.statusCode());assertTrue(me.body().contains(actors.get(name).getEmail()));return c;
    }
    HttpResponse<String> permissions(HttpClient c,Role role,List<Integer> ids) throws Exception {return send(c,"PUT","/api/roles/"+role.getId()+"/permissions",Map.of("permissionIds",ids));}
    String statusPath(String who) {return "/api/users/"+actors.get(who).getId()+"/status";}
    String rolePath(String who) {return "/api/users/"+actors.get(who).getId()+"/role";}
    int logs(){return jdbc.queryForObject("SELECT COUNT(*) FROM system_logs WHERE action IN ('UPDATE_ROLE_PERMISSIONS','UPDATE_USER','CHANGE_USER_STATUS','CHANGE_USER_ROLE','UPDATE_STAFF','CHANGE_STAFF_STATUS','UPDATE_MEMBER','CHANGE_MEMBER_STATUS')",Integer.class);}
    Map<String,Object> state() {
        return Map.of("users",jdbc.queryForList("SELECT user_id,full_name,role_id,status FROM users ORDER BY user_id"),"permissions",jdbc.queryForList("SELECT role_id,permission_id FROM role_permissions ORDER BY role_id,permission_id"),"audit",logs());
    }
    void conflict(HttpResponse<String> response) throws Exception { assertEquals(409,response.statusCode(),response.body());assertEquals("LAST_ADMIN_REQUIRED",json.readTree(response.body()).path("code").asText()); }
    long capable(){return jdbc.queryForObject("SELECT COUNT(*)"+UserRepository.CAPABLE_ADMIN,Long.class);}
    void sole() {tx(()->em.find(User.class,actors.get("admin-b").getId()).setStatus("Inactive"));}

    @ParameterizedTest @ValueSource(strings={"CenterManager","Receptionist","Coach","Member","no-mp"})
    void putRequiresAdminAndActualAuthority(String who) throws Exception {
        var c=login(who);var before=state();var r=permissions(c,ordinary,List.of());assertEquals(403,r.statusCode(),r.body());assertEquals(before,state());
        int get=who.equals("no-mp")?200:403;
        for(String path:List.of("/api/roles","/api/roles/"+main.getId(),"/api/permissions"))assertEquals(get,send(c,"GET",path,null).statusCode());
    }
    @Test void inactiveAdminExistingSessionCannotMutate() throws Exception {
        var c=login("admin-b");tx(()->em.find(User.class,actors.get("admin-b").getId()).setStatus("Inactive"));var before=state();
        var r=permissions(c,ordinary,List.of());assertEquals(403,r.statusCode());assertEquals("ACCOUNT_NOT_ACTIVE",json.readTree(r.body()).path("code").asText());assertEquals(before,state());
    }
    @Test void permissionsValidateAtomicallyAndAllowEmptyAndRetryOnOtherRole() throws Exception {
        var c=login("admin-a");
        for(Object ids:List.of(Arrays.asList(mu.getId(),2147483000),Arrays.asList((Integer)null),List.of(-1),List.of(0))) {
            var before=state();var r=send(c,"PUT","/api/roles/"+ordinary.getId()+"/permissions",Map.of("permissionIds",ids));assertEquals(400,r.statusCode());assertEquals(before,state());
        }
        for(var body:List.of(Map.of(),Collections.singletonMap("permissionIds",null))) {var before=state();assertEquals(400,send(c,"PUT","/api/roles/"+ordinary.getId()+"/permissions",body).statusCode());assertEquals(before,state());}
        assertEquals(404,send(c,"PUT","/api/roles/2147483000/permissions",Map.of("permissionIds",List.of())).statusCode());
        assertEquals(200,permissions(c,ordinary,List.of(mp.getId(),mp.getId())).statusCode());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions WHERE role_id=?",Integer.class,ordinary.getId()));
        int before=logs();for(int i=0;i<2;i++) {var r=permissions(c,ordinary,List.of());assertEquals(200,r.statusCode());assertEquals(0,json.readTree(r.body()).path("permissions").size());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions WHERE role_id=?",Integer.class,ordinary.getId()));}
        assertEquals(before+2,logs());assertTrue(capable()>=1);
        assertEquals(actors.get("admin-a").getId(),jdbc.queryForObject("SELECT user_id FROM system_logs WHERE action='UPDATE_ROLE_PERMISSIONS' ORDER BY log_id DESC LIMIT 1",Integer.class));
    }
    @Test void changingSharedAdminRoleMustCountAllAccountsAndBothAuthoritiesTogether() throws Exception {
        var c=login("admin-a");assertEquals(2,capable());
        for(var ids:List.of(List.<Integer>of(),List.of(mu.getId()),List.of(mp.getId()))) {var before=state();conflict(permissions(c,main,ids));assertEquals(before,state());}
        // Inactive spare and active Admin with only MP cannot preserve effective administration.
        assertEquals(2,capable());
    }
    @ParameterizedTest @ValueSource(strings={"Inactive","Locked","Suspended"})
    void lastAdminStatusIsRejectedAndRetryRemainsRejected(String status) throws Exception {
        sole();var c=login("admin-a");var before=state();for(int i=0;i<2;i++){conflict(send(c,"PATCH",statusPath("admin-a"),Map.of("status",status)));assertEquals(before,state());}
    }
    @Test void lastAdminRoleAndPutCannotBypassGuardOrPartiallySave() throws Exception {
        sole();var c=login("admin-a");var before=state();
        conflict(send(c,"PATCH",rolePath("admin-a"),Map.of("roleId",ordinary.getId())));assertEquals(before,state());
        for(var changes:List.of(Map.of("status","Inactive"),Map.of("roleId",ordinary.getId()),Map.of("roleName","Ordinary"))) {
            var body=new LinkedHashMap<String,Object>();body.put("fullName","Must roll back");body.put("email",actors.get("admin-a").getEmail());body.putAll(changes);
            conflict(send(c,"PUT","/api/users/"+actors.get("admin-a").getId(),body));assertEquals(before,state());
        }
    }
    @Test void aliasesCannotChangeAdmin() throws Exception {
        sole();var c=login("admin-a");var before=state();
        for(String group:List.of("staff","members"))for(String method:List.of("PATCH","PUT")){
            String path="/api/"+group+"/"+actors.get("admin-a").getId()+(method.equals("PATCH")?"/status":"");
            var r=send(c,method,path,Map.of("name","Alias rejected","email",actors.get("admin-a").getEmail(),"role","Coach","status","Inactive"));assertEquals(403,r.statusCode(),r.body());assertEquals(before,state());
        }
    }
    @Test void legitimateStatusAndRoleChangesCommitAndExistingSessionsLoseAccess() throws Exception {
        var a=login("admin-a");var b=login("admin-b");int before=logs();
        var inactive=send(a,"PATCH",statusPath("admin-b"),Map.of("status","Inactive"));assertEquals(200,inactive.statusCode());assertEquals("Inactive",json.readTree(inactive.body()).path("status").asText());
        assertEquals("Inactive",jdbc.queryForObject("SELECT status FROM users WHERE user_id=?",String.class,actors.get("admin-b").getId()));assertEquals(before+1,logs());
        assertEquals(403,send(b,"GET","/api/auth/me",null).statusCode());
        assertEquals(200,send(a,"PATCH",statusPath("admin-b"),Map.of("status","Active")).statusCode());b=login("admin-b");
        var demoted=send(a,"PATCH",rolePath("admin-b"),Map.of("roleId",ordinary.getId()));assertEquals(200,demoted.statusCode());assertEquals("Ordinary",json.readTree(demoted.body()).path("roleName").asText());
        assertEquals(ordinary.getId(),jdbc.queryForObject("SELECT role_id FROM users WHERE user_id=?",Integer.class,actors.get("admin-b").getId()));
        var changed=send(b,"GET","/api/auth/me",null);assertEquals(403,changed.statusCode());assertTrue(changed.body().contains("SESSION_ROLE_CHANGED"));
        assertEquals(401,send(b,"GET","/api/auth/me",null).statusCode());assertEquals(1,capable());
    }
    @Test void permissionChangesRefreshEverySessionOfAffectedRoleAndCanRemoveOwnAuthorityWhenSpareExists() throws Exception {
        var a=login("admin-a");var b=login("admin-b");tx(()->em.find(User.class,actors.get("spare").getId()).setStatus("Active"));
        var r=permissions(a,main,List.of(mu.getId()));assertEquals(200,r.statusCode());assertEquals(1,capable());
        for(var c:List.of(a,b)) {var me=send(c,"GET","/api/auth/me",null);assertEquals(200,me.statusCode());assertFalse(me.body().contains("MANAGE_PERMISSIONS"));var before=state();assertEquals(403,permissions(c,ordinary,List.of()).statusCode());assertEquals(before,state());}
        assertEquals(200,permissions(login("spare"),main,List.of(mu.getId(),mp.getId(),mr.getId())).statusCode());assertTrue(send(a,"GET","/api/auth/me",null).body().contains("MANAGE_PERMISSIONS"));
    }
    @Test void combinedPutCommitsWhenAnotherCapableAdminRemains() throws Exception {
        var c=login("admin-a");int before=logs();var r=send(c,"PUT","/api/users/"+actors.get("admin-b").getId(),Map.of("fullName","Updated DEMO","email",actors.get("admin-b").getEmail(),"roleName","Ordinary","status","Inactive"));
        assertEquals(200,r.statusCode(),r.body());var response=json.readTree(r.body());assertEquals("Ordinary",response.path("roleName").asText());assertEquals("Inactive",response.path("status").asText());assertEquals("Updated DEMO",response.path("fullName").asText());
        var saved=jdbc.queryForMap("SELECT full_name,role_id,status FROM users WHERE user_id=?",actors.get("admin-b").getId());assertEquals("Updated DEMO",saved.get("full_name"));assertEquals(ordinary.getId(),saved.get("role_id"));assertEquals("Inactive",saved.get("status"));assertEquals(before+1,logs());assertEquals(1,capable());
    }
    @ParameterizedTest @ValueSource(strings={"status-status","role-role","permission-status","permission-permission"})
    void concurrentMutationsShareGlobalLockAndCannotRemoveAllAdministrators(String scenario) throws Exception {
        // Separate role IDs for mixed/permission races expose a target-role-only lock bug.
        if(scenario.startsWith("permission"))tx(()->{em.find(User.class,actors.get("spare").getId()).setStatus("Active");em.find(User.class,actors.get("admin-b").getId()).setStatus("Inactive");});
        var a=login("partial"); // Admin MP-only may change permissions, but is NOT a capable administrator.
        var full=login("admin-a");var secondActor=scenario.startsWith("permission")?full:login("admin-b");var before=state();int auditBefore=logs();var pool=Executors.newFixedThreadPool(2);
        try(Connection blocker=jdbc.getDataSource().getConnection()) {
            blocker.setAutoCommit(false);blocker.createStatement().executeQuery("SELECT role_id FROM roles ORDER BY role_id FOR UPDATE");
            Future<HttpResponse<String>> first=pool.submit(()->switch(scenario){case "status-status"->send(full,"PATCH",statusPath("admin-a"),Map.of("status","Inactive"));case "role-role"->send(full,"PATCH",rolePath("admin-a"),Map.of("roleId",ordinary.getId()));default->permissions(a,main,List.of());});
            Future<HttpResponse<String>> second=pool.submit(()->switch(scenario){case "status-status"->send(secondActor,"PATCH",statusPath("admin-b"),Map.of("status","Inactive"));case "role-role"->send(secondActor,"PATCH",rolePath("admin-b"),Map.of("roleId",ordinary.getId()));case "permission-status"->send(full,"PATCH",statusPath("spare"),Map.of("status","Inactive"));default->permissions(a,spare,List.of());});
            int waiting=awaitBlocked(2);blocker.commit();assertEquals(2,waiting,"Both HTTP requests must reach the common administration lock");
            var one=first.get(40,TimeUnit.SECONDS);var two=second.get(40,TimeUnit.SECONDS);var statuses=new ArrayList<>(List.of(one.statusCode(),two.statusCode()));Collections.sort(statuses);
            // If the first request removes its actor's role/authority, the queued request must re-authorize.
            assertEquals(200,(int)statuses.get(0),one.body()+two.body());assertTrue(statuses.get(1)==409||statuses.get(1)==403,one.body()+two.body());
            if(!scenario.equals("permission-status")) {assertEquals(409,(int)statuses.get(1));conflict(one.statusCode()==409?one:two);}
            assertTrue(capable()>=1);assertEquals(auditBefore+1,logs());assertNotEquals(before,state());
            evidence.add(Map.of("scenario",scenario,"responses",statuses,"waiting",waiting,"capableAfter",capable(),"auditDelta",logs()-auditBefore));
        } finally {pool.shutdownNow();}
    }
    int awaitBlocked(int required) throws Exception {
        boolean h2=System.getProperty("scms.last-admin.pg-url", "").isBlank();String q=h2?"SELECT count(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL":"SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'";
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);int count=0;while(System.nanoTime()<deadline){count=jdbc.queryForObject(q,Integer.class);if(count>=required)return count;Thread.sleep(30);}return count;
    }
    @AfterAll void export() throws Exception {
        String output=System.getProperty("scms.last-admin.evidence", "");if(!output.isBlank())Files.writeString(Path.of(output),json.writerWithDefaultPrettyPrinter().writeValueAsString(evidence));
        String schema=System.getProperty("scms.last-admin.openapi", "");if(!schema.isBlank()){var c=HttpClient.newHttpClient();var r=send(c,"GET","/v3/api-docs",null);assertEquals(200,r.statusCode());Files.writeString(Path.of(schema),r.body());}
    }
}
