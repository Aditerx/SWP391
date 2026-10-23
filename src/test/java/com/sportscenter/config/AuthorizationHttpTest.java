package com.sportscenter.config;

import com.sportscenter.session.*;
import com.sportscenter.training.AttendanceService;
import com.sportscenter.user.UserRepository;
import com.sportscenter.invoice.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value={SessionController.class, InvoiceController.class}, properties={"scms.frontend-origins=https://frontend.test", "debug=false", "logging.level.org.springframework=WARN"}) @Import(SecurityConfig.class)
class AuthorizationHttpTest {
    @Autowired MockMvc mvc;
    @MockBean SessionService sessions;
    @MockBean AttendanceService attendance;
    @MockBean UserRepository users;
    @MockBean InvoiceService invoices;
    @MockBean PaymentProperties properties;
    static final String BOOK = "{\"subscriptionId\":1,\"coachId\":2,\"roomId\":1,\"sessionDate\":\"2026-12-01\",\"startTime\":\"06:00\",\"endTime\":\"07:30\",\"classId\":1}";
    @Test void anonymousScheduleRequestIs401() throws Exception { mvc.perform(get("/api/sessions")).andExpect(status().isUnauthorized()); }
    @Test void coachCannotBookForMember() throws Exception { mvc.perform(post("/api/sessions/book-private").with(user("coach").roles("COACH")).contentType("application/json").content(BOOK)).andExpect(status().isForbidden()); }
    @Test void receptionistWithOperationalPermissionCanBook() throws Exception { mvc.perform(post("/api/sessions/book-private").with(user("staff").authorities(new SimpleGrantedAuthority("REGISTER_MEMBER"))).contentType("application/json").content(BOOK)).andExpect(status().isCreated()); }
    @Test void centerManagerWithPermissionCanBook() throws Exception { mvc.perform(post("/api/sessions/book-private").with(user("manager").authorities(new SimpleGrantedAuthority("ROLE_CENTERMANAGER"),new SimpleGrantedAuthority("MANAGE_CLASSES"))).contentType("application/json").content(BOOK)).andExpect(status().isCreated()); }
    @Test void managerCannotConfirmCashWithoutReceptionistRole() throws Exception { mvc.perform(post("/api/invoices/1/confirm-cash").with(user("manager").authorities(new SimpleGrantedAuthority("ROLE_CENTERMANAGER"),new SimpleGrantedAuthority("MANAGE_INVOICES")))).andExpect(status().isForbidden()); }
    @Test void receptionistWithInvoicePermissionCanConfirmCash() throws Exception { mvc.perform(post("/api/invoices/1/confirm-cash").with(user("staff").authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"),new SimpleGrantedAuthority("MANAGE_INVOICES")))).andExpect(status().isOk()); }
    @Test void configuredFrontendCanUseCredentialedCors() throws Exception { mvc.perform(options("/api/sessions").header("Origin","https://frontend.test").header("Access-Control-Request-Method","GET")).andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","https://frontend.test")).andExpect(header().string("Access-Control-Allow-Credentials","true")); }
    @Test void unrelatedOriginIsRejected() throws Exception { mvc.perform(options("/api/sessions").header("Origin","https://untrusted.test").header("Access-Control-Request-Method","GET")).andExpect(status().isForbidden()); }
}
