package com.sportscenter.audit;

import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.membership.*;
import com.sportscenter.room.RoomRepository;
import com.sportscenter.session.*;
import com.sportscenter.sportclass.*;
import com.sportscenter.subject.SubjectRepository;
import com.sportscenter.user.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Explicit opt-in preparation of persistent DEMO records; never resets an existing password. */
@SpringBootTest(properties = {"debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=OFF", "spring.jpa.show-sql=false", "spring.datasource.hikari.maximum-pool-size=2", "spring.datasource.hikari.minimum-idle=0"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIfSystemProperty(named="scms.prepare.demo", matches="true")
class DemoDataPreparationTest {
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CoachRepository coaches;
    @Autowired ReceptionistRepository receptionists;
    @Autowired PasswordEncoder encoder;
    @Autowired MembershipPackageRepository packages;
    @Autowired SportsClassRepository classes;
    @Autowired SportsClassService classService;
    @Autowired SubjectRepository subjects;
    @Autowired RoomRepository rooms;
    @Autowired SessionService sessions;
    @Autowired SessionRepository sessionRepository;
    @Autowired EntityManager em;
    @MockBean PaymentExpirationJob worker;

    private User account(String role, String email, String label) {
        User existing = users.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if (!role.equals(existing.getRole().getName())) throw new IllegalStateException("Demo email has another role; stop and resolve: " + email);
            return existing;
        }
        User user = new User(); user.setFullName(label); user.setEmail(email);
        user.setRole(roles.findByNameIgnoreCase(role).orElseThrow());
        user.setCenterId(1); user.setStatus("Active"); user.setFirstLogin(false);
        user.setPasswordHash(encoder.encode("DemoCash2026"));
        users.saveAndFlush(user);
        if ("Coach".equals(role)) { Coach c = new Coach(); c.setUser(user); coaches.saveAndFlush(c); }
        if ("Receptionist".equals(role)) { Receptionist r = new Receptionist(); r.setUser(user); receptionists.saveAndFlush(r); }
        if ("CenterManager".equals(role)) em.createNativeQuery("INSERT INTO center_managers(user_id) VALUES (:id)").setParameter("id", user.getId()).executeUpdate();
        return user;
    }

    @Test @Transactional @Commit
    void prepareIsolatedDemoAccountsClassesAndSessions() {
        account("CenterManager", "demo.cash.manager.1010@example.test", "DEMO CASH Manager");
        account("Receptionist", "demo.cash.reception.1010@example.test", "DEMO CASH Lễ tân");
        User coach = account("Coach", "demo.cash.coach.1010@example.test", "DEMO CASH Mentor");
        if (packages.findAll().stream().noneMatch(p -> "DEMO CASH 30 ngày".equals(p.getName()))) {
            MembershipPackage p = new MembershipPackage(); p.setName("DEMO CASH 30 ngày");
            p.setPrice(new BigDecimal("300000")); p.setDurationDays(30); p.setStatus("Active");
            p.setBenefits("Gói thành viên dùng cho demo Cash"); packages.saveAndFlush(p);
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        int subject = subjects.findAll().stream().filter(s -> "Active".equals(s.getStatus())).findFirst().orElseThrow().getId();
        int room = rooms.findAll().stream().filter(r -> "Available".equals(r.getStatus()) && r.getCapacity() >= 4).findFirst().orElseThrow().getId();
        for (int n = 0; n < 2; n++) {
            String name = "DEMO CASH " + (n == 0 ? "Có phí " : "Miễn phí ") + today;
            var sportsClass = classes.findAll().stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
            int classId = sportsClass != null ? sportsClass.getId() : classService.create(new SportsClassRequest(
                    name, subject, coach.getId(), 4, BigDecimal.valueOf(n == 0 ? 150000 : 0), today, today.plusDays(7), "Ongoing")).id();
            if (sessionRepository.findFiltered(classId, null, null, today, today, null).isEmpty()) {
                boolean scheduled = false;
                for (TimeSlot slot : TimeSlot.values()) {
                    LocalTime start = slot.getStartTime(), end = slot.getEndTime();
                    if (!sessions.checkConflict(new ConflictCheckRequest(classId, coach.getId(), room, today, start, end, null)).hasConflict()) {
                        sessions.create(new SessionRequest(classId, room, today, start, end, "Scheduled")); scheduled = true; break;
                    }
                }
                if (!scheduled) throw new IllegalStateException("No demo room/coach slot available today; choose another room/date manually");
            }
        }
        em.flush();
        System.out.println("DEMO CASH prepared: 3 staff accounts, one 30-day package, paid/free classes and today's sessions. Existing accounts/passwords unchanged.");
    }
}
