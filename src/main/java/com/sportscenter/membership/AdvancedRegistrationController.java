package com.sportscenter.membership;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class AdvancedRegistrationController {
    private final AdvancedRegistrationService service;
    public record Request(@NotNull Integer subjectId,@NotNull Integer primaryCoachId,@NotBlank String requestKey) { }
    public record Decision(boolean accepted, String reason) { }
    public record Order(@NotBlank @Pattern(regexp="Cash|VNPay") String paymentMethod) { }
    public record Work(@NotNull Set<String> weeklySlots,@NotNull Set<LocalDate> daysOff,boolean acceptingNew) { }
    @GetMapping("/coaches/{id}/work-schedule") @PreAuthorize("isAuthenticated()")
    public AdvancedRegistrationService.WorkResponse work(@PathVariable Integer id,Authentication auth) { return service.work(id,auth); }
    @GetMapping("/coaches/{id}/admission-capacity") @PreAuthorize("isAuthenticated()")
    public CoachCapacityService.Capacity capacity(@PathVariable Integer id,@RequestParam Integer subjectId,Authentication auth) { return service.capacity(id,subjectId,auth); }
    @GetMapping("/advanced-registrations") @PreAuthorize("isAuthenticated()")
    public List<AdvancedRegistrationResponse> list(Authentication auth) { return service.list(auth); }
    @PostMapping("/advanced-registrations") @PreAuthorize("hasRole('MEMBER')")
    public AdvancedRegistrationResponse request(@Valid @RequestBody Request r,Authentication auth) { return service.request(r.subjectId(),r.primaryCoachId(),r.requestKey(),auth); }
    @PostMapping("/advanced-registrations/{id}/decision") @PreAuthorize("hasAnyRole('COACH','CENTERMANAGER','ADMIN')")
    public AdvancedRegistrationResponse decide(@PathVariable Integer id,@Valid @RequestBody Decision r,Authentication auth) { return service.decide(id,r.accepted(),r.reason(),auth); }
    @PostMapping("/advanced-registrations/{id}/order") @PreAuthorize("hasRole('MEMBER')")
    public SubscriptionOrderResponse order(@PathVariable Integer id,@Valid @RequestBody Order r,Authentication auth,HttpServletRequest request) { return service.order(id,r.paymentMethod(),request.getRemoteAddr(),auth); }
    @PutMapping("/coaches/{id}/work-schedule") @PreAuthorize("hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')")
    public void work(@PathVariable Integer id,@Valid @RequestBody Work r,Authentication auth) { service.configure(id,r.weeklySlots(),r.daysOff(),r.acceptingNew(),auth); }
}
