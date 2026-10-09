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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new LegacyPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository repository) {
        // Session authentication keeps this permission snapshot until the user logs in again.
        return email -> {
            User user = repository.findForAuthentication(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            if (!"Active".equalsIgnoreCase(user.getStatus())) {
                throw new UsernameNotFoundException("User is not active");
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            if (user.getRole() != null) {
                String roleName = user.getRole().getName();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));

                user.getRole().getPermissions().stream()
                        .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                        .forEach(authorities::add);

            }


            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPasswordHash())
                    .authorities(authorities)
                    .build();
        };
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    FirstLoginPasswordFilter firstLoginPasswordFilter(UserRepository userRepository) {
        return new FirstLoginPasswordFilter(userRepository);
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
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
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                             SecurityContextRepository securityContextRepository,
                                             CorsConfigurationSource corsConfigurationSource,
                                             FirstLoginPasswordFilter firstLoginPasswordFilter) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/login", "/api/auth/logout",
                                "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/register",
                                "/api/auth/verify-registration", "/api/auth/resend-otp").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/", "/index.html", "/assets/**", "/*.ico", "/*.png", "/*.svg", "/*.js", "/*.css", "/*.json").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .addFilterBefore(firstLoginPasswordFilter, AuthorizationFilter.class)
                .build();
    }
}
