package com.sportscenter.config;

import com.sportscenter.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Recheck persisted account/permissions before method authorization, including existing sessions. */
@RequiredArgsConstructor
public class CurrentAccountFilter extends OncePerRequestFilter {
    private final UserRepository users;
    private final SessionRegistry sessions;
    private final SecurityContextRepository contexts;

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath().isEmpty() ? request.getRequestURI() : request.getServletPath();
        if (!path.startsWith("/api/") || path.equals("/api/health")) return true;
        if ("GET".equals(request.getMethod())) return path.startsWith("/api/public/")
                || path.equals("/api/payments/vnpay/ipn") || path.equals("/api/payments/vnpay/return");
        return "POST".equals(request.getMethod()) && Set.of("/api/auth/login", "/api/auth/google",
                "/api/auth/logout", "/api/auth/register", "/api/auth/verify-registration", "/api/auth/resend-otp",
                "/api/auth/forgot-password", "/api/auth/reset-password").contains(path);
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws ServletException, IOException {
        var old = SecurityContextHolder.getContext().getAuthentication();
        if (old != null && old.isAuthenticated() && !"anonymousUser".equals(old.getName())) {
            var session = request.getSession(false);
            var information = session == null ? null : sessions.getSessionInformation(session.getId());
            if (information != null && information.isExpired()) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                response.sendError(401);
                return;
            }
            var account = users.findForAuthentication(old.getName()).orElse(null);
            if (account == null || !"Active".equalsIgnoreCase(account.getStatus()) || account.getRole() == null) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"code\":\"ACCOUNT_NOT_ACTIVE\",\"message\":\"Active account required\"}");
                return;
            }
            var authorities = new ArrayList<SimpleGrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + account.getRole().getName().toUpperCase(java.util.Locale.ROOT)));
            String role = authorities.get(0).getAuthority();
            if (old.getAuthorities().stream().anyMatch(a -> a.getAuthority().startsWith("ROLE_") && !role.equals(a.getAuthority()))) {
                if (session != null) session.invalidate();
                SecurityContextHolder.clearContext();
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"code\":\"SESSION_ROLE_CHANGED\",\"message\":\"Sign in again after the account role changed\"}");
                return;
            }
            account.getRole().getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getName())));
            if (!new HashSet<>(old.getAuthorities()).equals(new HashSet<>(authorities))) {
                var updated = new UsernamePasswordAuthenticationToken(old.getPrincipal(), null, authorities);
                updated.setDetails(old.getDetails());
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(updated);
                SecurityContextHolder.setContext(context);
                contexts.saveContext(context, request, response);
            }
        }
        chain.doFilter(request, response);
    }
}
