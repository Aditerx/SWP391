package com.sportscenter.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.training.AttendanceFinalizationJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in HTTP acceptance against a NEW local DB provisioned from the approved SQL. */
@EnabledIfSystemProperty(named="scms.demo.pg-url",matches="jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_schema_[a-z0-9_]+")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.config.import=", "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false"
})
class DemoSchemaLoginPostgresTest {
    @DynamicPropertySource
    static void localDatabase(DynamicPropertyRegistry registry) {
        String url=System.getProperty("scms.demo.pg-url", "");
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_schema_[a-z0-9_]+")) throw new IllegalArgumentException("Disposable local schema DB required");
        registry.add("spring.datasource.url",()->url);
        registry.add("spring.datasource.username",()->"postgres");
        registry.add("spring.datasource.password",()->"scms-local-only");
        registry.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
        registry.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect");
    }
    @MockBean PaymentExpirationJob paymentJob;
    @MockBean PackageExpirationJob packageJob;
    @MockBean AttendanceFinalizationJob attendanceJob;
    @MockBean NotificationScheduleJob notificationJob;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @LocalServerPort int port;

    @Test
    void everyPdfAccountCanLoginReadItsSessionAndUseItsRoleWithoutPasswordChange() throws Exception {
        List<Map<String,Object>> accounts=jdbc.queryForList("SELECT u.user_id,u.email,u.center_id,r.role_name FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.status='Active' ORDER BY u.user_id");
        assertEquals(33,accounts.size());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM users WHERE status='Active' AND (is_first_login OR failed_login_attempts<>0 OR locked_until IS NOT NULL)",Integer.class));
        Map<String,String> routes=Map.of("Member","/api/members/me","Coach","/api/coaches",
                "Receptionist","/api/invoices","CenterManager","/api/rooms","Admin","/api/users");
        for (var account:accounts) {
            String email=account.get("email").toString();
            String role=account.get("role_name").toString();
            HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
            JsonNode login=call(client,"POST","/api/auth/login",Map.of("email",email,"password","SWP391"),200);
            assertEquals(email,login.path("email").asText());
            assertFalse(login.path("isFirstLogin").asBoolean());
            JsonNode me=call(client,"GET","/api/auth/me",null,200);
            assertEquals(((Number)account.get("user_id")).intValue(),me.path("userId").asInt());
            assertEquals(email,me.path("email").asText());
            assertEquals(role,me.path("role").asText());
            call(client,"GET",routes.get(role),null,200);
            call(client,"POST","/api/auth/logout",null,204);
            call(client,"GET","/api/auth/me",null,401);
        }
        assertEquals(33,jdbc.queryForObject("SELECT count(*) FROM users WHERE status='Active' AND NOT is_first_login AND failed_login_attempts=0 AND locked_until IS NULL",Integer.class));
    }

    private JsonNode call(HttpClient client,String method,String path,Object body,int expected) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Accept","application/json");
        if (body==null) request.method(method,HttpRequest.BodyPublishers.noBody());
        else request.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var response=client.send(request.build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(expected,response.statusCode(),method+" "+path+" => "+response.body());
        return response.body().isBlank()?json.getNodeFactory().nullNode():json.readTree(response.body());
    }
}
