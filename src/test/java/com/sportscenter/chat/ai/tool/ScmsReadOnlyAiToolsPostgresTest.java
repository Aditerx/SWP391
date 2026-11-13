package com.sportscenter.chat.ai.tool;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.chat.ai.ChatAiProperties;
import com.sportscenter.config.ClockConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Opt-in query proof against a prebuilt, isolated loopback PostgreSQL schema. */
@EnabledIfSystemProperty(named = "scms.ai-tools.postgres-test-url",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/scms_ai_tools_[a-z0-9_]+")
@DataJpaTest(properties = {"spring.config.import=", "spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=never", "debug=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ScmsReadOnlyAiTools.class, ChatAiProperties.class, ClockConfig.class,
        org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class})
class ScmsReadOnlyAiToolsPostgresTest {
    @Autowired ScmsReadOnlyAiTools tools;

    @DynamicPropertySource
    static void isolatedPostgres(DynamicPropertyRegistry properties) {
        String url = System.getProperty("scms.ai-tools.postgres-test-url", "");
        if (!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/scms_ai_tools_[a-z0-9_]+"))
            throw new IllegalArgumentException("Only an isolated loopback PostgreSQL database is allowed");
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> System.getProperty("scms.ai-tools.postgres-test-password", "local-proof-only"));
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test void allToolsExecuteAgainstPostgresWithOptionalArgumentsOmitted() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, ToolContext> contexts = Map.of(
                "get_membership_packages", new ToolContext(null),
                "get_subjects", new ToolContext(null),
                "get_open_classes", new ToolContext(null),
                "get_my_subscriptions", new ToolContext(1),
                "get_my_upcoming_sessions", new ToolContext(1, "Tuần này tôi có buổi học nào?"),
                "get_my_recent_invoices", new ToolContext(1));
        for (AiTool tool : tools.all())
            assertNotNull(tool.execute(contexts.get(tool.name()), mapper.createObjectNode()), tool.name());
    }

    @Test void openClassesExecuteWithBlankAndConcreteSubjectFilters() {
        ObjectMapper mapper = new ObjectMapper();
        AiTool tool = tools.all().stream().filter(candidate -> candidate.name().equals("get_open_classes")).findFirst().orElseThrow();
        assertNotNull(tool.execute(new ToolContext(null), mapper.createObjectNode().put("subjectName", "   ")));
        assertNotNull(tool.execute(new ToolContext(null), mapper.createObjectNode().put("subjectName", "  Tennis  ")));
    }
}
