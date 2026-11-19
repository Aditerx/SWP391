package com.sportscenter.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/private-session-proposals") @RequiredArgsConstructor
public class PrivateSessionProposalController {
    private final PrivateSessionProposalService service;
    @io.swagger.v3.oas.annotations.media.Schema(name="PrivateSessionProposalDecision")
    public record Decision(@NotNull Boolean accepted) { }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COACH') or (hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES'))")
    public ResponseEntity<PrivateSessionProposalResponse> propose(@Valid @RequestBody PrivateBookingRequest r,Authentication auth){return ResponseEntity.status(HttpStatus.CREATED).body(service.propose(r,auth));}
    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER','COACH') or (hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES'))")
    public List<PrivateSessionProposalResponse> list(Authentication auth){return service.list(auth);}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER','COACH') or (hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES'))")
    public PrivateSessionProposalResponse get(@PathVariable Integer id,Authentication auth){return service.get(id,auth);}
    @PostMapping("/{id}/respond") @PreAuthorize("hasRole('MEMBER')")
    public PrivateSessionProposalResponse respond(@PathVariable Integer id,@Valid @RequestBody Decision r,Authentication auth){return service.respond(id,r.accepted(),auth);}
}
