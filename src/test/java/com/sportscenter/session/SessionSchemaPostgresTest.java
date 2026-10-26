package com.sportscenter.session;

import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.training.AttendanceFinalizationJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in against disposable local PostgreSQL, provisioned by the companion Python script. */
@EnabledIfSystemProperty(named = "scms.postgres.schema-test", matches = "true")
@SpringBootTest(properties = {"spring.config.import=", "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.datasource.username=postgres", "spring.datasource.password=scms-local-test",
        "spring.datasource.hikari.read-only=true", "spring.sql.init.mode=never",
        "debug=false", "logging.level.root=WARN", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional(readOnly = true)
class SessionSchemaPostgresTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getProperty("scms.postgres.test-url", "");
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55439/scms_f0607_(fresh|upgrade)")) {
            throw new IllegalArgumentException("Only the isolated local F06/F07 database is allowed");
        }
        registry.add("spring.datasource.url", () -> url);
    }

    @Autowired MockMvc mvc;
    @Autowired UserDetailsService userDetails;
    @Autowired SessionRepository sessions;
    @MockBean PaymentExpirationJob payments;
    @MockBean PackageExpirationJob packages;
    @MockBean AttendanceFinalizationJob attendance;

    @Test void hibernateReadsSessionWithDdlDisabled() {
        assertFalse(sessions.findAll().isEmpty());
        assertFalse(sessions.findFiltered(null, null, null, null, null, null).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"manager@scms.com", "receptionist@scms.com", "member@scms.com", "coach@scms.com"})
    void sessionListWorksWithDatabaseAuthorities(String email) throws Exception {
        mvc.perform(get("/api/sessions").with(user(userDetails.loadUserByUsername(email))))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test void managerReadsDashboardAndControlReports() throws Exception {
        var manager = user(userDetails.loadUserByUsername("manager@scms.com"));
        mvc.perform(get("/api/reports/dashboard").with(manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.todaySessions").isNumber());
        mvc.perform(get("/api/reports/members").with(manager)).andExpect(status().isOk());
        mvc.perform(get("/api/reports/revenue").with(manager)).andExpect(status().isOk());
    }

    @Test void anonymousCannotReadScheduleOrDashboard() throws Exception {
        mvc.perform(get("/api/sessions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reports/dashboard")).andExpect(status().isUnauthorized());
    }
}
