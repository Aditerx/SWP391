package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GoogleAuthServiceTest {
    private final GoogleTokenVerifier verifier = mock(GoogleTokenVerifier.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final TemporaryPasswordGenerator passwords = mock(TemporaryPasswordGenerator.class);
    private final AuditService audit = mock(AuditService.class);
    private GoogleAuthService service;

    @BeforeEach
    void setUp() {
        reset(verifier, users, roles, members, passwords, audit);
        service = new GoogleAuthService(verifier, users, roles, members, passwords, audit,
                Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));
        ReflectionTestUtils.setField(service, "autoRegister", true);
        when(verifier.verify(any())).thenReturn(identity("sub-1", "member@example.com", true, "Picture"));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void activeExistingUserGetsLinked() {
        User user = user("Active");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        assertSame(user, service.authenticate("credential"));
        assertEquals("sub-1", user.getGoogleSub());
        assertEquals("Picture", user.getAvatarUrl());
        verify(users).save(user);
    }

    @Test
    void rejectsUnverifiedEmail() {
        when(verifier.verify(any())).thenReturn(identity("sub-1", "member@example.com", false, null));
        assertLoginFailed(() -> service.authenticate("bad"));
        verifyNoInteractions(users);
    }

    @Test
    void rejectsInvalidToken() {
        when(verifier.verify(any())).thenThrow(new BadCredentialsException("Login failed"));
        assertLoginFailed(() -> service.authenticate("bad"));
    }

    @Test
    void autoRegistersMemberAndMemberRow() {
        Role role = new Role();
        role.setName("Member");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.empty());
        when(roles.findByNameIgnoreCase("Member")).thenReturn(Optional.of(role));
        when(passwords.generate()).thenReturn("temporary");
        when(passwords.hash("temporary")).thenReturn("hash");
        when(users.save(any(User.class))).thenAnswer(inv -> { User saved = inv.getArgument(0); saved.setId(7); return saved; });
        User registered = service.authenticate("credential");
        assertEquals("Member", registered.getRole().getName());
        assertEquals("Active", registered.getStatus());
        assertEquals("sub-1", registered.getGoogleSub());
        verify(members).save(argThat(m -> m.getUser() == registered && m.getJoinDate() != null));
    }

    @Test
    void rejectsWhenAutoRegisterDisabled() {
        ReflectionTestUtils.setField(service, "autoRegister", false);
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.empty());
        BusinessException error = assertThrows(BusinessException.class, () -> service.authenticate("credential"));
        assertEquals("Tài khoản chưa đăng ký", error.getMessage());
    }

    @Test
    void rejectsNonActiveAccountsWithoutMutatingThem() {
        for (String status : new String[]{"Pending", "Locked", "Inactive"}) {
            reset(users);
            User user = user(status);
            when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
            when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.of(user));
            assertLoginFailed(() -> service.authenticate("credential"));
            assertNull(user.getGoogleSub());
            verify(users, never()).save(any());
        }
    }

    @Test
    void rejectsDifferentExistingGoogleSubject() {
        User user = user("Active");
        user.setGoogleSub("different-sub");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.of(user));
        assertLoginFailed(() -> service.authenticate("credential"));
        verify(users, never()).save(any());
    }

    @Test
    void secondLoginFindsUserBySubject() {
        User user = user("Active");
        user.setGoogleSub("sub-1");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(user));
        assertSame(user, service.authenticate("credential"));
        verify(users, never()).findByEmailIgnoreCaseForUpdate(any());
    }

    @Test
    void fillsMissingAvatarFromPicture() {
        User user = user("Active");
        user.setGoogleSub("sub-1");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);
        service.authenticate("credential");
        assertEquals("Picture", user.getAvatarUrl());
    }

    private GoogleIdentity identity(String sub, String email, boolean verified, String picture) {
        return new GoogleIdentity(sub, email, verified, "Member", picture);
    }

    private User user(String status) {
        User user = new User(); user.setId(3); user.setEmail("member@example.com"); user.setStatus(status); return user;
    }

    private void assertLoginFailed(org.junit.jupiter.api.function.Executable action) {
        BadCredentialsException error = assertThrows(BadCredentialsException.class, action);
        assertEquals("Login failed", error.getMessage());
    }
}
