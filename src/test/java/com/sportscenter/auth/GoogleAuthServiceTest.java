package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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
    void createsMemberOnceAndMarksOnlyCreationCallAsNew() {
        Role role = new Role(); role.setName("Member");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.empty());
        when(roles.findByNameIgnoreCase("Member")).thenReturn(Optional.of(role));
        when(passwords.generate()).thenReturn("temporary");
        when(passwords.hash("temporary")).thenReturn("hash");
        when(users.save(any(User.class))).thenAnswer(inv -> { User saved = inv.getArgument(0); saved.setId(7); return saved; });

        GoogleAuthResult created = service.authenticate("credential");
        User registered = created.user();
        assertTrue(created.newAccount());
        assertEquals("Member", registered.getRole().getName());
        assertEquals("Active", registered.getStatus());
        assertFalse(registered.isFirstLogin());
        assertEquals("sub-1", registered.getGoogleSub());
        assertEquals("Picture", registered.getAvatarUrl());
        verify(members).save(argThat(m -> m.getUser() == registered && m.getJoinDate() != null));
        verify(audit).log(7, "REGISTER_MEMBER_GOOGLE", "USER", 7, "Google sign-in");

        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(registered));
        GoogleAuthResult second = service.authenticate("credential");
        assertFalse(second.newAccount());
        assertSame(registered, second.user());
        verify(members, times(1)).save(any(Member.class));
    }

    @Test
    void existingActiveUserLinksGoogleWithoutMarkingNew() {
        User user = user("Active");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.of(user));
        GoogleAuthResult result = service.authenticate("credential");
        assertFalse(result.newAccount());
        assertSame(user, result.user());
        assertEquals("sub-1", user.getGoogleSub());
        assertEquals("Picture", user.getAvatarUrl());
        verify(users).save(user);
        verify(members, never()).save(any());
        verify(audit).log(3, "LOGIN_GOOGLE", "USER", 3, "Google sign-in");
    }

    @Test
    void activeStaffRoleIsPreservedAndNeverCreatesStaffAccounts() {
        for (String roleName : new String[]{"Admin", "Manager", "Receptionist", "Coach"}) {
            reset(users, members);
            User user = user("Active");
            Role role = new Role(); role.setName(roleName); user.setRole(role);
            when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
            when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.of(user));
            when(users.save(user)).thenReturn(user);
            GoogleAuthResult result = service.authenticate("credential");
            assertFalse(result.newAccount());
            assertEquals(roleName, result.user().getRole().getName());
            verify(members, never()).save(any());
        }
    }

    @Test
    void pendingAccountIsRejectedWithoutMutation() {
        assertStatus("Pending", HttpStatus.CONFLICT, "ACCOUNT_PENDING_VERIFICATION");
    }

    @Test
    void lockedInactiveAndUnknownAccountsAreRejectedWithoutMutation() {
        for (String status : new String[]{"Locked", "Suspended", "Inactive", "Unexpected"}) {
            reset(users);
            User user = user(status);
            when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
            when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.of(user));
            assertCode(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", () -> service.authenticate("credential"));
            assertNull(user.getGoogleSub());
            verify(users, never()).save(any());
        }
    }

    @Test
    void conflictingGoogleSubjectReturnsConflictWithoutMutation() {
        User user = user("Active"); user.setGoogleSub("other-sub");
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.of(user));
        assertCode(HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_CONFLICT", () -> service.authenticate("credential"));
        verify(users, never()).save(any());
    }

    @Test
    void rejectsUnverifiedEmailAndMalformedIdentityWithSpecificCodes() {
        when(verifier.verify(any())).thenReturn(identity("sub-1", "member@example.com", false, null));
        assertCode(HttpStatus.UNAUTHORIZED, "GOOGLE_EMAIL_NOT_VERIFIED", () -> service.authenticate("token"));
        verifyNoInteractions(users);
        when(verifier.verify(any())).thenReturn(identity(null, "member@example.com", true, null));
        assertCode(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", () -> service.authenticate("token"));
    }

    @Test
    void invalidVerifierFailureMapsToTokenInvalid() {
        when(verifier.verify(any())).thenThrow(new BadCredentialsException("Login failed"));
        assertCode(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", () -> service.authenticate("bad"));
    }

    @Test
    void blankCredentialHasSpecificBadRequest() {
        assertCode(HttpStatus.BAD_REQUEST, "GOOGLE_CREDENTIAL_MISSING", () -> service.authenticate("  "));
        verifyNoInteractions(verifier);
    }

    @Test
    void noAccountAndAutoRegisterDisabledReturnsNotFound() {
        ReflectionTestUtils.setField(service, "autoRegister", false);
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.empty());
        assertCode(HttpStatus.NOT_FOUND, "GOOGLE_ACCOUNT_NOT_FOUND", () -> service.authenticate("credential"));
        verifyNoInteractions(roles, members);
    }

    private void assertStatus(String status, HttpStatus httpStatus, String code) {
        reset(users);
        User user = user(status);
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCaseForUpdate("member@example.com")).thenReturn(Optional.of(user));
        assertCode(httpStatus, code, () -> service.authenticate("credential"));
        assertNull(user.getGoogleSub());
        verify(users, never()).save(any());
    }

    private void assertCode(HttpStatus status, String code, org.junit.jupiter.api.function.Executable action) {
        GoogleAuthException error = assertThrows(GoogleAuthException.class, action);
        assertEquals(status, error.getStatus());
        assertEquals(code, error.getCode());
    }

    private GoogleIdentity identity(String sub, String email, boolean verified, String picture) {
        return new GoogleIdentity(sub, email, verified, "Member", picture);
    }

    private User user(String status) {
        User user = new User(); user.setId(3); user.setEmail("member@example.com"); user.setStatus(status); return user;
    }
}
