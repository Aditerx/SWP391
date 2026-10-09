package com.sportscenter.invoice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_INVOICES') or hasRole('MEMBER')")
    public ResponseEntity<List<InvoiceResponse>> findAll(
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer packageId,
            @RequestParam(required = false) Integer receptionistId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return ResponseEntity.ok(invoiceService.findAll(memberId, packageId, receptionistId, status, method, start, end, authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_INVOICES') or hasRole('MEMBER')")
    public ResponseEntity<InvoiceResponse> findById(@PathVariable Integer id, Authentication authentication) {
        return ResponseEntity.ok(invoiceService.findById(id, authentication));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_INVOICES')")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(request));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('MANAGE_INVOICES')")
    public ResponseEntity<InvoiceResponse> payInvoice(
            @PathVariable Integer id,
            @Valid @RequestBody InvoicePaymentRequest request
    ) {
        return ResponseEntity.ok(invoiceService.payInvoice(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_INVOICES')")
    public ResponseEntity<InvoiceResponse> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody InvoiceStatusRequest request
    ) {
        return ResponseEntity.ok(invoiceService.updateStatus(id, request.status()));
    }
}
