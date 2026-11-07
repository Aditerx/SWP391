package com.sportscenter.invoice;

import com.sportscenter.config.SecurityConfig;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller authorization only; approval/completion business behavior is not mocked into acceptance. */
@WebMvcTest(value = PaymentResolutionController.class, properties = {
        "spring.config.import=", "debug=false", "logging.level.org.springframework=WARN"
})
@Import(SecurityConfig.class)
class PaymentResolutionAuthorizationHttpTest {
    @org.junit.jupiter.api.BeforeEach void accountFixture() { com.sportscenter.config.ControllerSecurityFixture.currentAccount(users); }
    @Autowired MockMvc mvc;
    @MockBean UserRepository users;
    @MockBean PaymentResolutionService service;

    @ParameterizedTest(name = "{0}, invoice authority={1}")
    @CsvSource({"MEMBER,false", "MEMBER,true", "COACH,false", "COACH,true",
            "RECEPTIONIST,false", "RECEPTIONIST,true", "CENTERMANAGER,false", "CENTERMANAGER,true",
            "ADMIN,false", "ADMIN,true"})
    void allFiveRolesNeedBothAllowedRoleAndInvoiceAuthority(String role, boolean invoiceAuthority) throws Exception {
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        if (invoiceAuthority) authorities.add(new SimpleGrantedAuthority("MANAGE_INVOICES"));
        var actor = user("local@test.invalid").authorities(authorities);
        boolean staff = invoiceAuthority && !role.equals("MEMBER") && !role.equals("COACH");
        boolean manager = staff && !role.equals("RECEPTIONIST");

        mvc.perform(get("/api/payment-resolutions").with(actor)).andExpect(status().is(staff ? 200 : 403));
        mvc.perform(post("/api/payment-resolutions").with(actor).contentType("application/json")
                        .content("{\"invoiceId\":1,\"type\":\"Refund\",\"reason\":\"Local fixture\"}"))
                .andExpect(status().is(staff ? 200 : 403));
        mvc.perform(post("/api/payment-resolutions/1/approve").with(actor))
                .andExpect(status().is(manager ? 200 : 403));
        mvc.perform(post("/api/payment-resolutions/1/complete").with(actor).contentType("application/json")
                        .content("{\"evidence\":\"Local evidence\",\"moneyNotReceived\":false}"))
                .andExpect(status().is(manager ? 200 : 403));

        if (staff) {
            verify(service).list(any()); verify(service).open(eq(1), eq("Refund"), eq("Local fixture"), any());
        }
        if (manager) {
            verify(service).approve(eq(1), any());
            verify(service).complete(eq(1), eq("Local evidence"), isNull(), eq(false), any());
        }
        verifyNoMoreInteractions(service);
    }
}
