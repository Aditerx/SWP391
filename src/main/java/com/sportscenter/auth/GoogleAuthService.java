package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.AccountStatus;
import com.sportscenter.user.Member;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.RoleRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {
    private final GoogleTokenVerifier tokenVerifier;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final AuditService auditService;
    private final Clock clock;
    @Value("${google.auto-register:true}")
    private boolean autoRegister;

    @Transactional
    public GoogleAuthResult authenticate(String credential) {
        if (credential == null || credential.isBlank()) throw error(HttpStatus.BAD_REQUEST,
                "GOOGLE_CREDENTIAL_MISSING", "Vui lòng cung cấp thông tin đăng nhập Google.");
        GoogleIdentity identity;
        try {
            identity = tokenVerifier.verify(credential);
        } catch (GoogleAuthException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidToken();
        }
        if (identity == null || identity.sub() == null || identity.sub().isBlank()
                || identity.email() == null || identity.email().isBlank()) throw invalidToken();
        if (!identity.emailVerified()) throw error(HttpStatus.UNAUTHORIZED,
                "GOOGLE_EMAIL_NOT_VERIFIED", "Email Google chưa được xác minh.");

        User user = userRepository.findByGoogleSub(identity.sub()).orElse(null);
        boolean registered = false;
        if (user == null) {
            user = userRepository.findByEmailIgnoreCaseForUpdate(identity.email().trim()).orElse(null);
        }

        if (user == null) {
            if (!autoRegister) throw error(HttpStatus.NOT_FOUND,
                    "GOOGLE_ACCOUNT_NOT_FOUND", "Email chưa được đăng ký.");
            user = new User();
            user.setEmail(identity.email().trim().toLowerCase());
            user.setFullName(identity.name() == null || identity.name().isBlank() ? identity.email() : identity.name());
            user.setRole(roleRepository.findByNameIgnoreCase("Member")
                    .orElseThrow(() -> new BusinessException("Member role not found")));
            user.setCenterId(1);
            user.setStatus("Active");
            user.setFirstLogin(false);
            user.setPasswordHash(temporaryPasswordGenerator.hash(temporaryPasswordGenerator.generate()));
            user.setGoogleSub(identity.sub());
            user.setAvatarUrl(identity.pictureUrl());
            user = userRepository.save(user);
            Member member = new Member();
            member.setUser(user);
            member.setJoinDate(LocalDate.now(clock));
            memberRepository.save(member);
            registered = true;
        } else {
            AccountStatus.AuthenticationState status = AccountStatus.forAuthentication(user.getStatus());
            if (status == AccountStatus.AuthenticationState.PENDING) throw error(HttpStatus.CONFLICT,
                    "ACCOUNT_PENDING_VERIFICATION", "Tài khoản đang chờ xác minh email.");
            if (status == AccountStatus.AuthenticationState.BLOCKED) throw error(HttpStatus.FORBIDDEN,
                    "ACCOUNT_LOCKED", "Tài khoản không thể đăng nhập.");
            if (user.getGoogleSub() != null && !user.getGoogleSub().equals(identity.sub())) throw error(
                    HttpStatus.CONFLICT, "GOOGLE_ACCOUNT_CONFLICT", "Tài khoản đã liên kết với Google khác.");
            if (user.getGoogleSub() == null) {
                user.setGoogleSub(identity.sub());
                if (user.getAvatarUrl() == null && identity.pictureUrl() != null) user.setAvatarUrl(identity.pictureUrl());
                user = userRepository.save(user);
            } else if (user.getAvatarUrl() == null && identity.pictureUrl() != null) {
                user.setAvatarUrl(identity.pictureUrl());
                user = userRepository.save(user);
            }
        }
        auditService.log(user.getId(), registered ? "REGISTER_MEMBER_GOOGLE" : "LOGIN_GOOGLE", "USER", user.getId(), "Google sign-in");
        return new GoogleAuthResult(user, registered);
    }

    private GoogleAuthException invalidToken() {
        return error(HttpStatus.UNAUTHORIZED, "GOOGLE_TOKEN_INVALID", "Không thể xác minh thông tin Google.");
    }

    private GoogleAuthException error(HttpStatus status, String code, String message) {
        return new GoogleAuthException(status, code, message);
    }
}
