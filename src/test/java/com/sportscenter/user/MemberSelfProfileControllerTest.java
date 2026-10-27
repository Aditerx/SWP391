package com.sportscenter.user;

import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.training.AttendanceFinalizationJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:member_profile;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never",
        "logging.level.org.springframework=WARN",
        "logging.level.org.hibernate=WARN",
        "debug=false"
})
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(MemberSelfProfileControllerTest.FixedTime.class)
class MemberSelfProfileControllerTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock testClock(){return Clock.fixed(Instant.parse("2026-10-27T01:00:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"));}
    }
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @SpyBean MemberRepository members;
    @Autowired RoleRepository roles;
    @Autowired MemberSelfProfileService service;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @MockBean PaymentExpirationJob paymentExpirationJob;
    @MockBean PackageExpirationJob packageExpirationJob;
    @MockBean AttendanceFinalizationJob attendanceFinalizationJob;
    @MockBean NotificationScheduleJob notificationScheduleJob;

    private User memberA;
    private User memberB;

    @BeforeEach
    void seed() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM members");
            jdbc.update("DELETE FROM users");
            jdbc.update("DELETE FROM roles");
            Role role = new Role();
            role.setName("Member");
            role = roles.save(role);
            memberA = createMember("member-a@test.invalid", "Alice", role, "A goal", "A note");
            memberB = createMember("member-b@test.invalid", "Bob", role, "B goal", "B note");
        });
    }

    private User createMember(String email, String name, Role role, String goal, String note) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        user.setPasswordHash("test-hash");
        user.setStatus("Active");
        user.setRole(role);
        user.setCenterId(1);
        user = users.saveAndFlush(user);
        Member member = new Member();
        member.setUser(user);
        member.setGoal(goal);
        member.setHealthNote(note);
        members.saveAndFlush(member);
        return user;
    }

    @Test
    void routeRequiresMemberAndLiteralMeResolvesOnlyAuthenticatedOwner() throws Exception {
        mvc.perform(get("/api/members/me")).andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
        mvc.perform(get("/api/members/me").with(user("coach@test.invalid").roles("COACH")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
        mvc.perform(get("/api/members/me").with(user(memberA.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.goal").value("A goal"))
                .andExpect(jsonPath("$.healthNote").value("A note"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/members/" + memberB.getId()).with(user(memberA.getEmail()).roles("MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchUpdatesOnlySuppliedFieldsAndIgnoresProtectedFieldsAndMemberIds() throws Exception {
        mvc.perform(patch("/api/members/me").with(user(memberA.getEmail()).roles("MEMBER"))
                        .contentType("application/json")
                        .content("{\"name\":\"Alice Updated\",\"phone\":\"0901234567\",\"address\":\"District 1\",\"dateOfBirth\":\"2000-02-03\",\"gender\":\"Female\",\"goal\":\"Run\",\"healthNote\":\"Knee\",\"memberId\":" + memberB.getId() + ",\"role\":\"Admin\",\"status\":\"Inactive\",\"centerId\":99,\"packageId\":88,\"credit\":900,\"amount\":1,\"email\":\"other@test.invalid\",\"password\":\"secret\",\"avatarUrl\":\"bad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.phone").value("0901234567"))
                .andExpect(jsonPath("$.dateOfBirth").value("2000-02-03"))
                .andExpect(jsonPath("$.goal").value("Run"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        User savedA = users.findById(memberA.getId()).orElseThrow();
        Member profileA = members.findById(memberA.getId()).orElseThrow();
        User savedB = users.findById(memberB.getId()).orElseThrow();
        assertEquals("Alice Updated", savedA.getFullName());
        assertEquals("Active", savedA.getStatus());
        assertEquals(1, savedA.getCenterId());
        assertEquals("Run", profileA.getGoal());
        assertEquals("B goal", members.findById(memberB.getId()).orElseThrow().getGoal());
        assertEquals("Bob", savedB.getFullName());
        mvc.perform(get("/api/members/me").with(user(memberA.getEmail()).roles("MEMBER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Alice Updated"));
    }

    @Test
    void emptyPatchIsNoOpAndNullClearsNullableFieldsWhileMissingFieldsRemainUntouched() throws Exception {
        mvc.perform(patch("/api/members/me").with(user(memberA.getEmail()).roles("MEMBER"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Alice"));
        mvc.perform(patch("/api/members/me").with(user(memberA.getEmail()).roles("MEMBER"))
                        .contentType("application/json")
                        .content("{\"phone\":null,\"address\":null,\"dateOfBirth\":null,\"gender\":null,\"goal\":null,\"healthNote\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.dateOfBirth").doesNotExist())
                .andExpect(jsonPath("$.gender").doesNotExist())
                .andExpect(jsonPath("$.goal").doesNotExist())
                .andExpect(jsonPath("$.healthNote").doesNotExist())
                .andExpect(jsonPath("$.name").value("Alice"));
        assertNull(users.findById(memberA.getId()).orElseThrow().getPhone());
        assertNull(members.findById(memberA.getId()).orElseThrow().getGoal());
    }

    @Test
    void rejectsInvalidNameLengthsGenderDateAndNonObjectBodies() throws Exception {
        var member = user(memberA.getEmail()).roles("MEMBER");
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("{\"name\":null}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NAME_REQUIRED"));
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("{\"gender\":\"female\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_GENDER"));
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("{\"dateOfBirth\":\"2026-10-28\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("FUTURE_DATE_OF_BIRTH"));
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("{\"dateOfBirth\":\"03/02/2000\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_DATE_OF_BIRTH"));
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("[]"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("null"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_PROFILE_BODY"));
    }

    @Test
    void rejectsValuesLongerThanSchemaLimits() throws Exception {
        var member = user(memberA.getEmail()).roles("MEMBER");
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("{\"phone\":\"" + "1".repeat(21) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHONE_TOO_LONG"));
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("{\"address\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ADDRESS_TOO_LONG"));
        mvc.perform(patch("/api/members/me").with(member).contentType("application/json").content("{\"goal\":\"" + "g".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("GOAL_TOO_LONG"));
    }

    @Test
    void userAndMemberChangesRollbackTogetherWhenMemberPersistenceFails() {
        doThrow(new IllegalStateException("forced test failure")).when(members).save(any(Member.class));
        try {
            assertThrows(IllegalStateException.class, () -> service.patch(memberA.getEmail(), toPatchDto("{\"name\":\"Changed\",\"goal\":\"Changed\"}")));
        } finally {
            reset(members);
        }
        assertEquals("Alice", users.findById(memberA.getId()).orElseThrow().getFullName());
        assertEquals("A goal", members.findById(memberA.getId()).orElseThrow().getGoal());
    }

    private com.sportscenter.user.dto.MemberSelfProfilePatch toPatchDto(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,
                    com.sportscenter.user.dto.MemberSelfProfilePatch.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
