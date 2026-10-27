package com.sportscenter.notification;

import com.sportscenter.common.exception.GlobalExceptionHandler;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import({GlobalExceptionHandler.class, NotificationControllerTest.TestSecurityConfiguration.class})
class NotificationControllerTest {
    @Autowired MockMvc mvc;
    @MockBean NotificationService notificationService;
    @MockBean UserRepository userRepository;

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mvc.perform(get("/api/notifications/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void everySignedInRoleCanListOwnNotifications() throws Exception {
        for (String role : List.of("MEMBER", "COACH", "RECEPTIONIST", "MANAGER")) {
            String email = role.toLowerCase() + "@example.com";
            User user = new User(); user.setId(role.hashCode()); user.setEmail(email);
            when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
            when(notificationService.listMine(eq(user.getId()), anyBoolean(), any(), any(Pageable.class)))
                    .thenReturn(Page.<NotificationResponse>empty());

            mvc.perform(get("/api/notifications/me").session(session(email, role)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void unreadFilterAndPaginationAreForwarded() throws Exception {
        String email = "member@example.com";
        User user = new User(); user.setId(1);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(notificationService.listMine(eq(1), eq(true), eq(NotificationType.PAYMENT_SUCCESS), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(1, 2), 0));

        mvc.perform(get("/api/notifications/me?unreadOnly=true&type=PAYMENT_SUCCESS&page=1&size=2")
                        .session(session(email, "MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(2));

        verify(notificationService).listMine(eq(1), eq(true), eq(NotificationType.PAYMENT_SUCCESS),
                argThat(pageable -> pageable.getPageNumber() == 1 && pageable.getPageSize() == 2
                        && pageable.getSort().isUnsorted()));
    }

    @Test
    void anotherUsersNotificationIsNotFoundWhenRead() throws Exception {
        String email = "user-a@example.com";
        User user = new User(); user.setId(10);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        doThrow(new NotificationNotFoundException()).when(notificationService).markRead(10, 55);

        mvc.perform(patch("/api/notifications/55/read").session(session(email, "MEMBER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    private MockHttpSession session(String email, String role) {
        var authentication = new UsernamePasswordAuthenticationToken(email, "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean
        SecurityFilterChain notificationTestSecurity(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                            response.sendError(HttpStatus.UNAUTHORIZED.value())))
                    .securityContext(context -> context.securityContextRepository(new HttpSessionSecurityContextRepository()))
                    .build();
        }
    }
}
