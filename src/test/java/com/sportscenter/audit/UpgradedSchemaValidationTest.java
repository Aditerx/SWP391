package com.sportscenter.audit;

import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.training.AttendanceFinalizationJob;
import com.sportscenter.notification.NotificationRepository;
import com.sportscenter.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Validates every Hibernate mapping on an upgraded local clone. No production URL or scheduled jobs. */
@EnabledIfSystemProperty(named="scms.upgrade.schema-proof",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.config.import=", "spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=validate",
        "spring.mail.host=127.0.0.1", "spring.mail.username=", "spring.mail.password=", "debug=false", "logging.level.root=WARN"})
class UpgradedSchemaValidationTest {
    @DynamicPropertySource static void localDatabase(DynamicPropertyRegistry r) {
        String url=System.getProperty("scms.upgrade.local-url","");
        if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_upgrade_[a-z0-9_]+"))
            throw new IllegalArgumentException("Only a guarded local production clone is permitted");
        r.add("spring.datasource.url",()->url);r.add("spring.datasource.username",()->"postgres");
        r.add("spring.datasource.password",()->"scms-local-only");
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
        r.add("spring.jpa.database-platform",()->"org.hibernate.dialect.PostgreSQLDialect");
    }
    @MockBean PaymentExpirationJob payments;
    @MockBean PackageExpirationJob packages;
    @MockBean AttendanceFinalizationJob attendance;
    @MockBean NotificationScheduleJob notifications;
    @MockBean JavaMailSenderImpl mail;
    @Autowired JdbcTemplate jdbc;
    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationService notificationService;
    @Autowired Clock clock;
    @Autowired TestRestTemplate http;

    @Test void allMappingsValidateAndApprovedCatalogIsPresent() {
        // Context startup with ddl-auto=validate is the mapping check; this reads committed migration output.
        assertEquals(9,jdbc.queryForObject("SELECT count(*) FROM subjects WHERE slug IS NOT NULL",Integer.class));
        var health=http.getForEntity("/actuator/health",String.class);
        assertEquals(200,health.getStatusCode().value());
        assertTrue(health.getBody().contains("\"components\""));
        assertFalse(health.getBody().contains("\"details\""));
    }

    @Test @Transactional
    void legacyBigintReferenceSurvivesEntityAndApiProjection() {
        Integer userId=jdbc.queryForObject("SELECT min(user_id) FROM users",Integer.class);
        long reference=3_000_000_000L;
        Integer id=jdbc.queryForObject("INSERT INTO notifications(user_id,title,content,type,related_id,is_read,created_at) "
                +"VALUES(?, 'Schema proof', 'Local rollback fixture', 'Other', ?, false, ?) RETURNING notification_id",
                Integer.class,userId,reference,LocalDateTime.now(clock));
        assertEquals(reference,notificationRepository.findById(id).orElseThrow().getRelatedId());
        var response=notificationService.listMine(userId,false,null,PageRequest.of(0,100)).stream()
                .filter(n->n.id().equals(id)).findFirst().orElseThrow();
        assertEquals(reference,response.relatedId());
    }
}
