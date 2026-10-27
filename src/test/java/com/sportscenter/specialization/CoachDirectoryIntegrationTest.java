package com.sportscenter.specialization;

import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.PackageExpirationJob;
import com.sportscenter.notification.NotificationScheduleJob;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.training.AttendanceFinalizationJob;
import com.sportscenter.user.Coach;
import com.sportscenter.user.CoachRepository;
import com.sportscenter.user.Role;
import com.sportscenter.user.RoleRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:coach_directory;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.sql.init.mode=never",
        "logging.level.org.springframework=WARN",
        "logging.level.org.hibernate=WARN",
        "debug=false"
})
@AutoConfigureMockMvc
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoachDirectoryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CoachRepository coaches;
    @Autowired CoachSpecializationRepository assignments;
    @Autowired SpecializationRepository specializations;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired SubjectRepository subjects;
    @Autowired ObjectMapper objectMapper;
    @Autowired EntityManagerFactory entityManagerFactory;
    @MockBean PaymentExpirationJob paymentExpirationJob;
    @MockBean PackageExpirationJob packageExpirationJob;
    @MockBean AttendanceFinalizationJob attendanceFinalizationJob;
    @MockBean NotificationScheduleJob notificationScheduleJob;

    private User manager;
    private User member;
    private User admin;
    private User listedCoach;
    private User secondCoach;
    private Specialization tennis;
    private Specialization swimming;

    @BeforeAll
    void seed() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO centers(center_id) VALUES (1), (2)");
            Role managerRole = role("CenterManager");
            Role memberRole = role("Member");
            Role coachRole = role("Coach");
            Role adminRole = role("Admin");
            manager = user("manager@directory.test", "Center Manager", managerRole, 1);
            member = user("member@directory.test", "Member", memberRole, 1);
            admin = user("admin@directory.test", "Admin", adminRole, null);

            Subject subject = new Subject();
            subject.setName("Test sport");
            subject.setSlug("directory-test-sport");
            subject = subjects.saveAndFlush(subject);
            tennis = specialization("Tennis", subject);
            swimming = specialization("Swimming", subject);
            Specialization football = specialization("Football", subject);

            listedCoach = addCoach("coach-one@directory.test", "Alice Coach", coachRole, 1);
            secondCoach = addCoach("coach-two@directory.test", "Bob Coach", coachRole, 1);
            addAssignment(listedCoach, tennis);
            addAssignment(listedCoach, swimming);
            addCoach("coach-three@directory.test", "Center Two Coach", coachRole, 2);
            addAssignment(users.findByEmailIgnoreCase("coach-three@directory.test").orElseThrow(), football);
            for (int index = 4; index <= 8; index++) {
                addCoach("coach-" + index + "@directory.test", "Extra Coach " + index, coachRole, 2);
            }
        });
    }

    @BeforeEach
    void restoreCenterFixture() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (User coach : List.of(listedCoach, secondCoach)) {
                User stored = users.findById(coach.getId()).orElseThrow();
                stored.setCenterId(1);
                users.save(stored);
            }
            for (int index = 3; index <= 8; index++) {
                User coach = users.findByEmailIgnoreCase(coachEmail(index)).orElseThrow();
                coach.setCenterId(2);
                users.save(coach);
            }
        });
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return roles.save(role);
    }

    private User user(String email, String name, Role role, Integer centerId) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        user.setRole(role);
        user.setCenterId(centerId);
        user.setStatus("Active");
        user.setPasswordHash("test-hash");
        return users.saveAndFlush(user);
    }

    private User addCoach(String email, String name, Role role, Integer centerId) {
        User user = user(email, name, role, centerId);
        Coach coach = new Coach();
        coach.setUser(user);
        coach.setBio("Bio " + name);
        coach.setCertification("Certificate");
        coaches.saveAndFlush(coach);
        return user;
    }

    private Specialization specialization(String name, Subject subject) {
        Specialization specialization = new Specialization();
        specialization.setName(name);
        specialization.setSubject(subject);
        return specializations.saveAndFlush(specialization);
    }

    private void addAssignment(User user, Specialization specialization) {
        Coach coach = coaches.findById(user.getId()).orElseThrow();
        assignments.saveAndFlush(new CoachSpecialization(coach, specialization));
    }

    private String coachEmail(int index) {
        return "coach-" + (index == 3 ? "three" : index) + "@directory.test";
    }

    @Test
    void directoryReturnsWholeSpecializationSetAndCombinedFilters() throws Exception {
        List<CoachSummaryResponse> all = request(manager.getEmail(), null, null, "ROLE_CENTERMANAGER");
        assertEquals(2, all.size());
        CoachSummaryResponse alice = all.stream().filter(coach -> coach.id().equals(listedCoach.getId())).findFirst().orElseThrow();
        CoachSummaryResponse bob = all.stream().filter(coach -> coach.id().equals(secondCoach.getId())).findFirst().orElseThrow();
        assertEquals(2, alice.specializations().size());
        assertTrue(alice.specializations().containsAll(List.of("Tennis", "Swimming")));
        assertTrue(bob.specializations().isEmpty());
        assertNull(alice.email());

        List<CoachSummaryResponse> filtered = request(manager.getEmail(), tennis.getId(), "  swimMING ", "ROLE_CENTERMANAGER");
        assertEquals(1, filtered.size());
        assertEquals(alice.id(), filtered.get(0).id());
        assertEquals(2, filtered.get(0).specializations().size());
        assertTrue(filtered.get(0).specializations().containsAll(List.of("Tennis", "Swimming")));
        assertTrue(request(manager.getEmail(), 999999, "none", "ROLE_CENTERMANAGER").isEmpty());
    }

    @Test
    void contactsRequireManageUsersAndCenterScopeIsPreservedForMemberManagerAndAdmin() throws Exception {
        assertEquals(2, request(member.getEmail(), null, null, "ROLE_MEMBER").size());
        CoachSummaryResponse hidden = request(manager.getEmail(), null, null, "ROLE_CENTERMANAGER").get(0);
        assertNull(hidden.email());
        CoachSummaryResponse visible = request(manager.getEmail(), null, null,
                "ROLE_CENTERMANAGER", "MANAGE_USERS").get(0);
        assertNotNull(visible.email());
        assertEquals(8, request(admin.getEmail(), null, null, "ROLE_ADMIN").size());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User missingCenter = user("missing-center@directory.test", "No Center", roles.findAll().stream()
                    .filter(role -> "Member".equals(role.getName())).findFirst().orElseThrow(), null);
        });
        mvc.perform(get("/api/coaches").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user("missing-center@directory.test").roles("MEMBER")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/coaches")).andExpect(status().isUnauthorized());
    }

    @Test
    void preparedStatementCountDoesNotGrowWithCoachCount() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        assertTrue(statistics.isStatisticsEnabled());
        statistics.clear();
        assertEquals(2, request(manager.getEmail(), null, null, "ROLE_CENTERMANAGER").size());
        long statementsForTwo = statistics.getPrepareStatementCount();
        System.out.println("D09 prepared statements for 2 coaches: " + statementsForTwo);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (int index = 3; index <= 8; index++) {
                User coach = users.findByEmailIgnoreCase(coachEmail(index)).orElseThrow();
                coach.setCenterId(1);
                users.save(coach);
            }
        });
        statistics.clear();
        assertEquals(8, request(manager.getEmail(), null, null, "ROLE_CENTERMANAGER").size());
        long statementsForEight = statistics.getPrepareStatementCount();
        System.out.println("D09 prepared statements for 8 coaches: " + statementsForEight);
        assertEquals(statementsForTwo, statementsForEight,
                "directory query count must stay constant as the number of coaches grows");
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mvc.perform(get("/api/coaches")).andExpect(status().isUnauthorized());
    }

    @Test
    void emptyCenterScopedDirectoryReturnsEmptyArray() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            for (User coach : users.findAll().stream().filter(user -> user.getEmail().startsWith("coach-")).toList()) {
                coach.setCenterId(2);
                users.save(coach);
            }
        });
        assertTrue(request(member.getEmail(), null, null, "ROLE_MEMBER").isEmpty());
    }

    private List<CoachSummaryResponse> request(String email, Integer specializationId, String keyword,
                                                String... authorities) throws Exception {
        var request = get("/api/coaches").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user(email).authorities(
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
        if (specializationId != null) request.param("specializationId", specializationId.toString());
        if (keyword != null) request.param("keyword", keyword);
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    }
}
