package com.sportscenter.auth;

import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import com.sportscenter.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleAuthControllerTest {
    @AfterEach
    void clearSecurityContexts() {
        SecurityContextHolder.clearContext();
        TestSecurityContextHolder.clearContext();
    }

    @Test
    void googleLoginResponseIncludesAvatarUrl() {
        AuthenticationManagerHolder holder = new AuthenticationManagerHolder();
        UserRepository users = mock(UserRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        GoogleAuthService google = mock(GoogleAuthService.class);
        User user = new User(); user.setId(42); user.setEmail("m@example.com"); user.setFullName("Member");
        user.setStatus("Active"); user.setAvatarUrl("https://res.cloudinary.com/demo/image/upload/avatar.jpg");
        com.sportscenter.user.Role role = new com.sportscenter.user.Role(); role.setName("Member"); user.setRole(role);
        UserDetails principal = org.springframework.security.core.userdetails.User.withUsername("m@example.com")
                .password("").authorities(new SimpleGrantedAuthority("ROLE_MEMBER")).build();
        UserDetailsService details = mock(UserDetailsService.class);
        when(google.authenticate("token")).thenReturn(user);
        when(details.loadUserByUsername("m@example.com")).thenReturn(principal);
        when(users.findForAuthentication("m@example.com")).thenReturn(Optional.of(user));
        when(members.existsById(42)).thenReturn(true);
        AuthController controller = new AuthController(holder.auth, mock(SecurityContextRepository.class), users,
                members, mock(LoginAttemptService.class), mock(UserService.class), details, mock(SessionRegistry.class),
                mock(PasswordResetService.class), mock(RegistrationService.class), mock(PasswordEncoder.class), google);
        var response = controller.google(new GoogleLoginRequest("token"), new MockHttpServletRequest(), new MockHttpServletResponse());
        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://res.cloudinary.com/demo/image/upload/f_auto,q_auto/avatar.jpg",
                response.getBody().avatarUrl());
    }

    private static class AuthenticationManagerHolder {
        final org.springframework.security.authentication.AuthenticationManager auth = mock(org.springframework.security.authentication.AuthenticationManager.class);
    }
}
