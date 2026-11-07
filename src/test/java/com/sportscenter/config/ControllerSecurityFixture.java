package com.sportscenter.config;

import com.sportscenter.user.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** In controller-only tests, persist the same synthetic fixture identity/permissions as the request. */
public final class ControllerSecurityFixture {
    private ControllerSecurityFixture() { }
    public static void currentAccount(UserRepository users) {
        when(users.findForAuthentication(anyString())).thenAnswer(invocation -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return Optional.empty();
            Role role = new Role();
            role.setName(auth.getAuthorities().stream().map(a -> a.getAuthority())
                    .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).findFirst().orElse("CENTERMANAGER"));
            auth.getAuthorities().stream().filter(a -> !a.getAuthority().startsWith("ROLE_")).forEach(a -> {
                Permission p = new Permission(); p.setName(a.getAuthority()); role.getPermissions().add(p);
            });
            User actor = new User(); actor.setId(1); actor.setEmail(invocation.getArgument(0));
            actor.setStatus("Active"); actor.setRole(role); actor.setCenterId(1);
            return Optional.of(actor);
        });
    }
}
