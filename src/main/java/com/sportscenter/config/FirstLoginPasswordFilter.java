package com.sportscenter.config;

import com.sportscenter.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class FirstLoginPasswordFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                && isRestrictedPath(request)
                && userRepository.findByEmailIgnoreCase(authentication.getName())
                        .map(user -> user.isFirstLogin()).orElse(false)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"Please change your password before continuing\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRestrictedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("GET".equals(method) && ("/api/payments/vnpay/ipn".equals(path)
                || "/api/payments/vnpay/return".equals(path))) return false;
        return !("POST".equals(method) && ("/api/auth/change-password".equals(path)
                || "/api/auth/logout".equals(path))
                || "GET".equals(method) && "/api/auth/me".equals(path));
    }
}
