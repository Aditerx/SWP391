package com.sportscenter.audit;

import com.sportscenter.feedback.ClassFeedbackRepository;
import com.sportscenter.invoice.PaymentExpirationJob;
import com.sportscenter.invoice.InvoiceRepository;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in live audit: SELECT-only, real PostgreSQL; the expiry worker is mocked. */
@SpringBootTest(properties = {"spring.datasource.hikari.read-only=true",
    "spring.datasource.hikari.connection-init-sql=SET default_transaction_read_only = on",
    "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=OFF",
    "spring.jpa.show-sql=false", "spring.datasource.hikari.maximum-pool-size=2", "spring.datasource.hikari.minimum-idle=0"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="scms.live.audit", matches="true")
class LiveReadOnlyAuditTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SportsClassRepository classes;
    @Autowired ClassFeedbackRepository feedback;
    @Autowired InvoiceRepository invoices;
    @MockBean PaymentExpirationJob expiryWorker;
    @Autowired com.sportscenter.enrollment.ClassEnrollmentRepository enrollments;

    @Test @Transactional(readOnly=true) void sessionSearchWithRealDateBoundsOnPostgres() throws Exception {
        var session=session(user("CenterManager"), "ROLE_CENTERMANAGER", "MANAGE_CLASSES");
        mvc.perform(get("/api/sessions").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/sessions").param("startDate", "2026-10-10").param("endDate", "2026-10-11").session(session)).andExpect(status().isOk());
    }

    private User user(String role) {
        return users.findAll().stream().filter(u -> "Active".equals(u.getStatus()) && !u.isFirstLogin()
            && u.getRole() != null && role.equals(u.getRole().getName())).findFirst().orElseThrow();
    }
    private MockHttpSession session(User u, String... grants) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(u.getEmail(), "audit-session",
            java.util.Arrays.stream(grants).map(SimpleGrantedAuthority::new).toList()));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
    @Test void publicCallbackRejectsUnsignedInputWithoutWrites() throws Exception {
        mvc.perform(get("/api/payments/vnpay/ipn")).andExpect(status().isOk()).andExpect(jsonPath("$.RspCode").value("97"));
        mvc.perform(post("/api/payments/vnpay/ipn")).andExpect(status().isUnauthorized());
    }
    @Test @Transactional(readOnly=true) void memberReadsOwnInvoicesThroughRealDatabase() throws Exception {
        mvc.perform(get("/api/members/me/invoices").session(session(user("Member"), "ROLE_MEMBER")))
            .andExpect(status().isOk());
    }
    @Test @Transactional(readOnly=true) void memberCannotUseStaffInvoiceList() throws Exception {
        mvc.perform(get("/api/invoices").session(session(user("Member"), "ROLE_MEMBER")))
            .andExpect(status().isForbidden());
    }
    @Test @Transactional(readOnly=true) void memberCannotUseStaffPurchaseRoute() throws Exception {
        User u=user("Member");
        mvc.perform(post("/api/members/"+u.getId()+"/packages").session(session(u,"ROLE_MEMBER"))
            .contentType("application/json").content("{\"packageId\":1,\"paymentMethod\":\"Cash\"}"))
            .andExpect(status().isForbidden());
    }
    @Test @Transactional(readOnly=true) void invoiceListSupportsEmptyFiltersOnPostgres() throws Exception {
        mvc.perform(get("/api/invoices").session(session(user("Receptionist"),"ROLE_RECEPTIONIST","MANAGE_INVOICES")))
            .andExpect(status().isOk());
    }
    @Test @Transactional(readOnly=true) void invoiceListSupportsStatusAndKeywordOnPostgres() throws Exception {
        mvc.perform(get("/api/invoices").param("status","Pending").param("keyword","INV-")
            .session(session(user("Receptionist"),"ROLE_RECEPTIONIST","MANAGE_INVOICES")))
            .andExpect(status().isOk());
    }
    @Test @Transactional(readOnly=true) void managerReadsClassFeedback() throws Exception {
        int id=classes.findAll().get(0).getId();
        mvc.perform(get("/api/classes/"+id+"/feedback")
            .session(session(user("CenterManager"),"ROLE_CENTERMANAGER","MANAGE_CLASSES")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.classId").value(id));
    }
    @Test @Transactional(readOnly=true) void coachReadsAnonymousFeedback() throws Exception {
        mvc.perform(get("/api/coaches/me/feedback").session(session(user("Coach"),"ROLE_COACH")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.feedback").isArray())
            .andExpect(jsonPath("$.feedback[*].memberId").doesNotExist());
    }
    @Test @Transactional(readOnly=true) void feedbackEligibilityQueryWorksOnRealSchema() {
        assertDoesNotThrow(() -> feedback.hasPresentAttendance(classes.findAll().get(0).getId(), user("Member").getId()));
    }
    @Test @Transactional(readOnly=true) void memberCanReadClassCatalogAndOwnEnrollments() throws Exception {
        var s=session(user("Member"),"ROLE_MEMBER");
        mvc.perform(get("/api/classes").session(s)).andExpect(status().isOk());
        mvc.perform(get("/api/enrollments").session(s)).andExpect(status().isOk());
    }
    @Test @Transactional(readOnly=true) void catalogGroupedCountsIncludePendingSeats() throws Exception {
        var result=mvc.perform(get("/api/classes").session(session(user("Member"),"ROLE_MEMBER"))).andExpect(status().isOk()).andReturn();
        var data=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
        for(var item:data) {
            assertEquals(enrollments.countBySportsClassIdAndStatusIn(item.get("id").asInt(), List.of("Registered","Pending")), item.get("enrolledCount").asLong());
        }
    }
    @Test @Transactional(readOnly=true) void invoiceSearchMustIncludeMembershipInvoices() {
        var expected=invoices.findAllWithDetails().stream().filter(i -> i.getMembershipPackage()!=null
            && "Paid".equals(i.getPaymentStatus()) && i.getPaymentMethod()!=null && i.getPaymentDate()!=null)
            .findFirst().orElseThrow();
        var found=invoices.searchInvoices(null,null,null,"Paid",expected.getPaymentMethod(),expected.getInvoiceCode(),
            java.time.LocalDateTime.of(2020,1,1,0,0),java.time.LocalDateTime.of(2099,12,31,0,0));
        assertTrue(found.stream().anyMatch(i -> expected.getInvoiceId().equals(i.getInvoiceId())),
            "Invoice search silently drops membership invoices because its class-name path creates an inner join");
    }
}
