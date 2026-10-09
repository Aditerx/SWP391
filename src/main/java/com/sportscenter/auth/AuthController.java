package com.sportscenter.auth;

import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionInformation;
import com.sportscenter.user.UserService;
import com.sportscenter.user.dto.ChangePasswordRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final SessionRegistry sessionRegistry;
    private final PasswordResetService passwordResetService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        String email = request.email().trim();
        if (loginAttemptService.isTemporarilyLocked(email)) {
            throw new BadCredentialsException("Login failed or account is temporarily locked");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException exception) {
            loginAttemptService.recordFailure(email);
            throw new BadCredentialsException("Login failed or account is temporarily locked");
        }
        loginAttemptService.recordSuccess(email);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            sessionRegistry.registerNewSession(session.getId(), authentication.getPrincipal());
        }

        User user = userRepository.findForAuthentication(email)
                .orElseThrow();
        return ResponseEntity.ok(toResponse(user, authentication));
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(Authentication authentication) {
        User user = userRepository.findForAuthentication(authentication.getName())
                .orElseThrow();
        return ResponseEntity.ok(toResponse(user, authentication));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication authentication,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        User user = userService.changePassword(authentication.getName(), request);
        UserDetails updatedPrincipal = userDetailsService.loadUserByUsername(user.getEmail());
        Authentication updatedAuthentication = new UsernamePasswordAuthenticationToken(
                updatedPrincipal, null, authentication.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(updatedAuthentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        HttpSession currentSession = httpRequest.getSession(false);
        for (SessionInformation session : sessionRegistry.getAllSessions(authentication.getPrincipal(), false)) {
            if (currentSession == null || !session.getSessionId().equals(currentSession.getId())) {
                session.expireNow();
            }
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<OtpMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(new OtpMessageResponse(PasswordResetService.GENERIC_FORGOT_MESSAGE));
    }

    @PostMapping("/register")
    public ResponseEntity<OtpMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(registrationService.register(request));
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<Void> verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest request) {
        registrationService.verify(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<OtpMessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        registrationService.resend(request);
        return ResponseEntity.ok(new OtpMessageResponse("Nếu thông tin hợp lệ, mã xác thực sẽ được gửi đến hộp thư."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request);
        return ResponseEntity.noContent().build();
    }

    private LoginResponse toResponse(User user, Authentication authentication) {
        String role = user.getRole() == null ? null : user.getRole().getName();
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                role,
                new LinkedHashSet<>(authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList()),
                user.isFirstLogin());
    }
}
