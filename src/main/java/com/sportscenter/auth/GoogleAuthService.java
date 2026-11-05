package com.sportscenter.auth;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.user.Member;
import com.sportscenter.user.MemberRepository;
import com.sportscenter.user.RoleRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
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
    public User authenticate(String credential) {
        GoogleIdentity identity = tokenVerifier.verify(credential);
        if (!identity.emailVerified() || identity.email() == null || identity.email().isBlank()
                || identity.sub() == null || identity.sub().isBlank()) throw denied();

        User user = userRepository.findByGoogleSub(identity.sub()).orElse(null);
        boolean registered = false;
        if (user == null) {
            user = userRepository.findByEmailIgnoreCaseForUpdate(identity.email().trim()).orElse(null);
        }

        if (user == null) {
            if (!autoRegister) throw new BusinessException("Tài khoản chưa đăng ký");
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
            if (!"Active".equalsIgnoreCase(user.getStatus())) throw denied();
            if (user.getGoogleSub() != null && !user.getGoogleSub().equals(identity.sub())) throw denied();
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
        return user;
    }

    private BadCredentialsException denied() { return new BadCredentialsException("Login failed"); }
}
