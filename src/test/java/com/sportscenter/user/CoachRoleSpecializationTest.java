package com.sportscenter.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.auth.EmailService;
import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.CoachCapacityService;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.specialization.Specialization;
import com.sportscenter.specialization.SpecializationRepository;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.training.AttendanceFinalizationJob;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP handlers/JPA commits: role changes never invent a Coach subject. */
@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:coach_role_specialization;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.sql.init.mode=never", "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoachRoleSpecializationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired SubjectRepository subjects;
    @Autowired SpecializationRepository specializations;
    @Autowired CoachCapacityService capacity;
    @Autowired PlatformTransactionManager transactions;
    @Autowired Clock clock;
    @MockBean EmailService email;
    @MockBean PaymentExpirationJob paymentJob;
    @MockBean PackageExpirationJob packageJob;
    @MockBean AttendanceFinalizationJob attendanceJob;
    @MockBean NotificationScheduleJob notificationJob;

    private Role coachRole;
    private Role memberRole;
    private Subject badminton;
    private Subject tennis;
    private Specialization badmintonSpecialization;

    @BeforeAll
    void fixture() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO centers(center_id) VALUES(1)");
            coachRole = role("Coach");
            memberRole = role("Member");
            User manager = account("manager@coach-role.test", role("CenterManager"));
            User member = account("member@coach-role.test", memberRole);
            jdbc.update("INSERT INTO members(user_id,join_date) VALUES(?,?)", member.getId(), LocalDate.now(clock));
            badminton = subject("Cầu lông", "cau-long");
            tennis = subject("Tennis", "quan-vot-tennis");
            badmintonSpecialization = new Specialization();
            badmintonSpecialization.setName("Cầu lông");
            badmintonSpecialization.setSubject(badminton);
            badmintonSpecialization = specializations.saveAndFlush(badmintonSpecialization);
            jdbc.update("INSERT INTO rooms(room_name,capacity,status,facility_type,center_id) VALUES('Sân Cầu lông 01',15,'Available','Indoor',1)");
            assertNotNull(manager.getId());
        });
    }

    @Test
    void creatingCoachWithoutSelectionHasNoSubjectAndCannotAcceptOrTeach() throws Exception {
        int id = createCoach(false);
        assertUnqualified(id);
        long registrations = jdbc.queryForObject("SELECT count(*) FROM advanced_registrations", Long.class);
        mvc.perform(post("/api/advanced-registrations").with(user("member@coach-role.test").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "subjectId", badminton.getId(), "primaryCoachId", id, "requestKey", UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("COACH_SUBJECT_MISMATCH"));
        assertEquals(registrations, jdbc.queryForObject("SELECT count(*) FROM advanced_registrations", Long.class));

        long classes = jdbc.queryForObject("SELECT count(*) FROM classes", Long.class);
        long sessions = jdbc.queryForObject("SELECT count(*) FROM sessions", Long.class);
        String requestId = UUID.randomUUID().toString();
        mvc.perform(post("/api/classes/normal-with-sessions").with(manager())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "requestId", requestId, "roomId", jdbc.queryForObject("SELECT room_id FROM rooms", Integer.class),
                                "daysOfWeek", List.of(1, 4), "startTime", "06:00", "endTime", "07:30",
                                "sportsClass", Map.of("name", "Cầu lông - Normal - K01", "subjectId", badminton.getId(),
                                        "coachId", id, "maxCapacity", 15, "classType", "Normal", "totalSessions", 12,
                                        "startDate", LocalDate.now(clock).plusDays(2).toString())))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("COACH_SUBJECT_MISMATCH"));
        assertEquals(classes, jdbc.queryForObject("SELECT count(*) FROM classes", Long.class));
        assertEquals(sessions, jdbc.queryForObject("SELECT count(*) FROM sessions", Long.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM normal_class_requests WHERE request_id=?", Integer.class, requestId));
    }

    @Test
    void patchingRoleCreatesUnqualifiedCoachWithoutInventingSpecialization() throws Exception {
        User target = new TransactionTemplate(transactions).execute(status -> account(UUID.randomUUID()+"@coach-role.test", memberRole));
        mvc.perform(patch("/api/users/{id}/role", target.getId()).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("roleId", coachRole.getId()))))
                .andExpect(status().isOk()).andExpect(jsonPath("roleName").value("Coach"));
        assertUnqualified(target.getId());
    }

    @Test
    void creatingCoachUsesOnlyExplicitlySelectedSpecialization() throws Exception {
        assertQualifiedOnlyForBadminton(createCoach(true));
    }

    @Test
    void changingRoleThroughUpdateCanAssignTheSelectedSubject() throws Exception {
        User target = new TransactionTemplate(transactions).execute(status -> account(UUID.randomUUID()+"@coach-role.test", memberRole));
        mvc.perform(put("/api/users/{id}", target.getId()).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fullName", target.getFullName(), "email", target.getEmail(),
                                "roleId", coachRole.getId(), "specializationIds", List.of(badmintonSpecialization.getId())))))
                .andExpect(status().isOk()).andExpect(jsonPath("roleName").value("Coach"));
        assertQualifiedOnlyForBadminton(target.getId());
    }

    private int createCoach(boolean selected) throws Exception {
        var body = new java.util.LinkedHashMap<String,Object>();
        body.put("fullName", "Coach Cầu lông");
        body.put("email", UUID.randomUUID()+"@coach-role.test");
        body.put("roleName", "Coach");
        if (selected) body.put("specializationIds", List.of(badmintonSpecialization.getId()));
        var result = mvc.perform(post("/api/users").with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).path("id").asInt();
    }

    private void assertUnqualified(int id) {
        assertNull(jdbc.queryForObject("SELECT specialization FROM coaches WHERE user_id=?", String.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM coach_specializations WHERE coach_id=?", Integer.class, id));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            var error = assertThrows(com.sportscenter.common.exception.BusinessException.class,
                    () -> capacity.requireCoach(users.findById(id).orElseThrow(), badminton, 1));
            assertTrue(error.getMessage().contains("COACH_SUBJECT_MISMATCH"));
        });
    }

    private void assertQualifiedOnlyForBadminton(int id) {
        assertNull(jdbc.queryForObject("SELECT specialization FROM coaches WHERE user_id=?", String.class, id));
        assertEquals(List.of(badmintonSpecialization.getId()), jdbc.queryForList("SELECT specialization_id FROM coach_specializations WHERE coach_id=?", Integer.class, id));
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            User coach = users.findById(id).orElseThrow();
            assertDoesNotThrow(() -> capacity.requireCoach(coach, badminton, 1));
            assertThrows(com.sportscenter.common.exception.BusinessException.class, () -> capacity.requireCoach(coach, tennis, 1));
        });
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor manager() {
        return user("manager@coach-role.test").authorities(new SimpleGrantedAuthority("ROLE_CENTERMANAGER"),
                new SimpleGrantedAuthority("MANAGE_USERS"), new SimpleGrantedAuthority("MANAGE_CLASSES"));
    }

    private Role role(String name) { Role role=new Role();role.setName(name);return roles.saveAndFlush(role); }
    private User account(String email, Role role) {
        User user=new User();user.setFullName("Role fixture");user.setEmail(email);user.setRole(role);
        user.setCenterId(1);user.setStatus("Active");user.setPasswordHash("unused-test-hash");return users.saveAndFlush(user);
    }
    private Subject subject(String name,String slug) {
        Subject subject=new Subject();subject.setName(name);subject.setSlug(slug);subject.setOutdoor(false);
        subject.setNormalPrice(new BigDecimal("900000"));subject.setAdvancedPrice(new BigDecimal("3000000"));return subjects.saveAndFlush(subject);
    }
}
