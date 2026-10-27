package com.sportscenter.session;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.time.LocalDateTime;
import java.util.List;
@RestController @RequestMapping("/api/center-incidents") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')")
public class CenterIncidentController {
    private final CenterIncidentService service;
    private final NormalMakeupService makeups;
    @io.swagger.v3.oas.annotations.media.Schema(name="CenterIncidentOpenRequest")
    public record Open(@jakarta.validation.constraints.NotNull Integer sessionId,@jakarta.validation.constraints.NotNull LocalDateTime startedAt,
            @jakarta.validation.constraints.NotBlank String reason,@jakarta.validation.constraints.NotBlank String evidence,@jakarta.validation.constraints.NotBlank String requestKey) { }
    public record Resolve(@jakarta.validation.constraints.NotNull LocalDateTime endedAt) { }
    public record Compensate(@jakarta.validation.constraints.NotBlank String reason,boolean verifiedNotHeld) { }
    @GetMapping public List<CenterIncidentService.Response> list(Authentication auth){return service.list(auth);}
    @PostMapping public CenterIncidentService.Response open(@jakarta.validation.Valid @RequestBody Open r,Authentication auth){return service.open(r.sessionId(),r.startedAt(),r.reason(),r.evidence(),r.requestKey(),auth);}
    @PostMapping("/{id}/resolve") public CenterIncidentService.Response resolve(@PathVariable Integer id,@jakarta.validation.Valid @RequestBody Resolve r,Authentication auth){return service.resolve(id,r.endedAt(),auth);}
    @PostMapping("/{id}/compensate") public CenterIncidentService.Response compensate(@PathVariable Integer id,@jakarta.validation.Valid @RequestBody Compensate r,Authentication auth){return service.compensateUnheldSession(id,r.reason(),r.verifiedNotHeld(),auth);}
    @PostMapping("/{id}/normal-makeup") public SessionResponse makeup(@PathVariable Integer id,@jakarta.validation.Valid @RequestBody NormalMakeupService.Request r,Authentication auth){return makeups.create(id,r,auth);}
}
