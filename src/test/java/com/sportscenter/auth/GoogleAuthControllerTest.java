package com.sportscenter.auth;

import com.sportscenter.common.exception.GlobalExceptionHandler;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.Role;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import com.sportscenter.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GoogleAuthControllerTest {
    private GoogleAuthService google;
    private UserRepository users;
    private MemberRepository members;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        google = mock(GoogleAuthService.class);
        users = mock(UserRepository.class);
        members = mock(MemberRepository.class);
        UserDetails principal = org.springframework.security.core.userdetails.User.withUsername("m@example.com")
                .password("").authorities(new SimpleGrantedAuthority("ROLE_MEMBER")).build();
        UserDetailsService details = mock(UserDetailsService.class);
        when(details.loadUserByUsername("m@example.com")).thenReturn(principal);
        User responseUser = user();
        when(users.findForAuthentication("m@example.com")).thenReturn(Optional.of(responseUser));
        when(members.existsById(42)).thenReturn(true);
        AuthController controller = new AuthController(mock(org.springframework.security.authentication.AuthenticationManager.class),
                mock(SecurityContextRepository.class), users, members, mock(LoginAttemptService.class), mock(UserService.class),
                details, mock(SessionRegistry.class), mock(PasswordResetService.class), mock(RegistrationService.class),
                mock(PasswordEncoder.class), google);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearSecurityContexts() {
        SecurityContextHolder.clearContext();
        TestSecurityContextHolder.clearContext();
    }

    @Test
    void newGoogleAccountResponseHasOneTimeFlagAndLeavesFirstLoginFalse() throws Exception {
        when(google.authenticate("token")).thenReturn(new GoogleAuthResult(user(), true), new GoogleAuthResult(user(), false));
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNewAccount").value(true))
                .andExpect(jsonPath("$.isFirstLogin").value(false))
                .andExpect(jsonPath("$.avatarUrl").value("https://res.cloudinary.com/demo/image/upload/f_auto,q_auto/avatar.jpg"));
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNewAccount").value(false))
                .andExpect(jsonPath("$.isFirstLogin").value(false));
    }

    @Test
    void controllerReturnsGoogleCodesWithTheirHttpStatuses() throws Exception {
        assertGoogleError("token", new GoogleAuthException(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", "Token không hợp lệ."));
        assertGoogleError("token", new GoogleAuthException(HttpStatus.UNAUTHORIZED, "GOOGLE_EMAIL_NOT_VERIFIED", "Email chưa xác minh."));
        assertGoogleError("token", new GoogleAuthException(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", "Tài khoản không thể đăng nhập."));
        assertGoogleError("token", new GoogleAuthException(HttpStatus.CONFLICT, "ACCOUNT_PENDING_VERIFICATION", "Tài khoản đang chờ xác minh."));
        assertGoogleError("token", new GoogleAuthException(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_CONFLICT", "Tài khoản đã liên kết."));
        assertGoogleError("token", new GoogleAuthException(HttpStatus.NOT_FOUND, "GOOGLE_ACCOUNT_NOT_FOUND", "Email chưa đăng ký."));
        assertGoogleError(null, new GoogleAuthException(HttpStatus.BAD_REQUEST, "GOOGLE_CREDENTIAL_MISSING", "Vui lòng cung cấp thông tin đăng nhập Google."));
    }

    private void assertGoogleError(String credential, GoogleAuthException exception) throws Exception {
        doThrow(exception).when(google).authenticate(credential);
        String body = credential == null ? "{}" : "{\"credential\":\"" + credential + "\"}";
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(exception.getStatus().value()))
                .andExpect(jsonPath("$.code").value(exception.getCode()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));
    }

    private User user() {
        User user = new User(); user.setId(42); user.setEmail("m@example.com"); user.setFullName("Member");
        user.setStatus("Active"); user.setFirstLogin(false);
        user.setAvatarUrl("https://res.cloudinary.com/demo/image/upload/avatar.jpg");
        Role role = new Role(); role.setName("Member"); user.setRole(role);
        return user;
    }
}
