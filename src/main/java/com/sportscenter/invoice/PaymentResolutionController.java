package com.sportscenter.invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.time.LocalDateTime;
import java.util.List;
@RestController @RequestMapping("/api/payment-resolutions") @RequiredArgsConstructor
public class PaymentResolutionController {
    private final PaymentResolutionService service;
    @io.swagger.v3.oas.annotations.media.Schema(name="PaymentResolutionOpenRequest")
    public record Open(@jakarta.validation.constraints.NotNull Integer invoiceId,@jakarta.validation.constraints.NotBlank String type,@jakarta.validation.constraints.NotBlank String reason) { }
    public record Complete(@jakarta.validation.constraints.NotBlank String evidence,LocalDateTime performedAt,boolean moneyNotReceived) { }
    @GetMapping @PreAuthorize("hasAuthority('MANAGE_INVOICES') and hasAnyRole('RECEPTIONIST','CENTERMANAGER','ADMIN')") public List<PaymentResolutionService.Response> list(Authentication auth){return service.list(auth);}
    @PostMapping @PreAuthorize("hasAuthority('MANAGE_INVOICES') and hasAnyRole('RECEPTIONIST','CENTERMANAGER','ADMIN')") public PaymentResolutionService.Response open(@jakarta.validation.Valid @RequestBody Open r,Authentication auth){return service.open(r.invoiceId(),r.type(),r.reason(),auth);}
    @PostMapping("/{id}/approve") @PreAuthorize("hasAuthority('MANAGE_INVOICES') and hasAnyRole('CENTERMANAGER','ADMIN')") public PaymentResolutionService.Response approve(@PathVariable Integer id,Authentication auth){return service.approve(id,auth);}
    @PostMapping("/{id}/complete") @PreAuthorize("hasAuthority('MANAGE_INVOICES') and hasAnyRole('CENTERMANAGER','ADMIN')") public PaymentResolutionService.Response complete(@PathVariable Integer id,@jakarta.validation.Valid @RequestBody Complete r,Authentication auth){return service.complete(id,r.evidence(),r.performedAt(),r.moneyNotReceived(),auth);}
}
