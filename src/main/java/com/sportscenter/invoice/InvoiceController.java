package com.sportscenter.invoice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<List<InvoiceResponse>> findAll(
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer packageId,
            @RequestParam(required = false) Integer receptionistId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return ResponseEntity.ok(invoiceService.findAll(memberId, packageId, receptionistId, status, method, start, end));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(invoiceService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PROCESS_PAYMENT', 'MANAGE_SUBSCRIPTIONS', 'MANAGE_PACKAGES')")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(request));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyAuthority('PROCESS_PAYMENT', 'MANAGE_SUBSCRIPTIONS')")
    public ResponseEntity<InvoiceResponse> payInvoice(
            @PathVariable Integer id,
            @Valid @RequestBody InvoicePaymentRequest request
    ) {
        return ResponseEntity.ok(invoiceService.payInvoice(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('PROCESS_PAYMENT', 'MANAGE_SUBSCRIPTIONS')")
    public ResponseEntity<InvoiceResponse> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody InvoiceStatusRequest request
    ) {
        return ResponseEntity.ok(invoiceService.updateStatus(id, request.status()));
    }
}
