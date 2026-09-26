package com.sportscenter.config;

import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new LegacyPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository repository) {
        return email -> {
            User user = repository.findForAuthentication(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            if (!"Active".equalsIgnoreCase(user.getStatus())) {
                throw new UsernameNotFoundException("User is not active");
            }

            Set<GrantedAuthority> authorities = new HashSet<>();
            if (user.getRole() != null) {
                user.getRole().getPermissions().stream()
                        .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                        .forEach(authorities::add);
                if ("CenterManager".equalsIgnoreCase(user.getRole().getName())) {
                    authorities.add(new SimpleGrantedAuthority("MANAGE_USERS"));
                    authorities.add(new SimpleGrantedAuthority("MANAGE_CLASSES"));
                    authorities.add(new SimpleGrantedAuthority("MANAGE_PACKAGES"));
                    authorities.add(new SimpleGrantedAuthority("VIEW_REPORTS"));
                    authorities.add(new SimpleGrantedAuthority("VIEW_AUDIT_LOG"));
                }
            }

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPasswordHash())
                    .authorities(authorities)
                    .build();
        };
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                             SecurityContextRepository securityContextRepository) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .build();
    }
}
