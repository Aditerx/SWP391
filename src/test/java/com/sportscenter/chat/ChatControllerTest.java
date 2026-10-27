package com.sportscenter.chat;

import com.sportscenter.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import({GlobalExceptionHandler.class,ChatControllerTest.TestSecurityConfiguration.class})
class ChatControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ChatService service;

    @Test void unauthenticatedRequestReturns401() throws Exception {
        mvc.perform(get("/api/chat/conversations/1/messages")).andExpect(status().isUnauthorized());
    }
    @Test void memberCanOpenChatEndpoint() throws Exception {
        when(service.getOrCreate(any(),any())).thenReturn(new ChatConversationResponse(1,4,"Member",ChatConversationStatus.OPEN,null,null,ChatHandlerType.STAFF,"Help",null,null,null,null,null,0));
        mvc.perform(post("/api/chat/conversations").session(session("m@example.com","MEMBER","CHAT_AS_MEMBER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }
    @Test void receptionistAndManagerCanAccessInbox() throws Exception {
        when(service.inbox(any(),any(),anyBoolean(),any(),any())).thenReturn(org.springframework.data.domain.Page.empty());
        for(String role:List.of("RECEPTIONIST","CENTERMANAGER"))
            mvc.perform(get("/api/chat/conversations").session(session(role.toLowerCase()+"@example.com",role,"HANDLE_CHAT")))
                    .andExpect(status().isOk());
    }
    @Test void coachIsDeniedInbox() throws Exception {
        mvc.perform(get("/api/chat/conversations").session(session("coach@example.com","COACH")))
                .andExpect(status().isForbidden());
    }
    @Test void memberIsDeniedStaffInbox() throws Exception {
        mvc.perform(get("/api/chat/conversations").session(session("member@example.com","MEMBER","CHAT_AS_MEMBER")))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession session(String email,String role,String... permissions) {
        var authorities=new java.util.ArrayList<SimpleGrantedAuthority>(); authorities.add(new SimpleGrantedAuthority("ROLE_"+role));
        for(String p:permissions) authorities.add(new SimpleGrantedAuthority(p));
        var authentication=new UsernamePasswordAuthenticationToken(email,"n/a",authorities);
        SecurityContext context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication);
        MockHttpSession session=new MockHttpSession(); session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context); return session;
    }
    @TestConfiguration @EnableMethodSecurity static class TestSecurityConfiguration {
        @Bean SecurityFilterChain chatTestSecurity(HttpSecurity http) throws Exception {
            return http.csrf(csrf->csrf.disable()).authorizeHttpRequests(auth->auth.anyRequest().authenticated())
                    .exceptionHandling(errors->errors.authenticationEntryPoint((request,response,exception)->response.sendError(HttpStatus.UNAUTHORIZED.value())))
                    .securityContext(context->context.securityContextRepository(new HttpSessionSecurityContextRepository())).build();
        }
    }
}
