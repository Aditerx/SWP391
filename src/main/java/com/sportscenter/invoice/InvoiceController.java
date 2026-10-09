package com.sportscenter.invoice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final PaymentProperties paymentProperties;

    @GetMapping("/api/invoices")
    @PreAuthorize("hasAuthority('MANAGE_INVOICES')")
    public ResponseEntity<List<InvoiceResponse>> findAll(
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false) Integer packageId,
            @RequestParam(required = false) Integer receptionistId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        return ResponseEntity.ok(invoiceService.findAll(memberId, packageId, receptionistId, status, method, keyword, start, end, authentication));
    }

    @GetMapping("/api/invoices/{id}")
    @PreAuthorize("hasAuthority('MANAGE_INVOICES')")
    public InvoiceResponse findById(@PathVariable Integer id, Authentication authentication) {
        return invoiceService.findById(id, authentication);
    }

    @GetMapping("/api/members/me/invoices")
    @PreAuthorize("hasRole('MEMBER')")
    public List<InvoiceResponse> ownInvoices(Authentication authentication) { return invoiceService.findOwn(authentication); }

    @GetMapping("/api/members/me/invoices/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    public InvoiceResponse ownInvoice(@PathVariable Integer id, Authentication authentication) { return invoiceService.findOwnById(id, authentication); }

    @PostMapping("/api/invoices/{id}/confirm-cash")
    @PreAuthorize("hasAuthority('MANAGE_INVOICES') and hasRole('RECEPTIONIST')")
    public InvoiceResponse confirmCash(@PathVariable Integer id, Authentication authentication) {
        return invoiceService.confirmCash(id, authentication);
    }

    @PostMapping("/api/invoices/{id}/cancel")
    @PreAuthorize("hasAuthority('MANAGE_INVOICES') or hasRole('MEMBER')")
    public InvoiceResponse cancel(@PathVariable Integer id, Authentication authentication) { return invoiceService.cancel(id, authentication); }

    @PostMapping("/api/invoices/{id}/pay-online")
    @PreAuthorize("hasRole('MEMBER')")
    public Map<String, String> payOnline(@PathVariable Integer id, Authentication authentication, HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return Map.of("paymentUrl", invoiceService.payOnline(id, authentication, ip));
    }

    @GetMapping("/api/payments/vnpay/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        InvoiceService.VnPayResult result = invoiceService.processVnpayResult(params);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("RspCode", result.code());
        response.put("Message", result.message());
        return response;
    }

    @GetMapping("/api/payments/vnpay/return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> params) {
        InvoiceService.VnPayResult result = invoiceService.processVnpayResult(params);
        String code = result.invoice() != null ? result.invoice().getInvoiceCode() : params.getOrDefault("vnp_TxnRef", "");
        RedirectView redirect = new RedirectView(paymentProperties.getFrontendResultUrl());
        redirect.setStatusCode(HttpStatus.FOUND);
        redirect.setExposeModelAttributes(false);
        String invoiceId = result.invoice() != null ? "&invoiceId=" + result.invoice().getInvoiceId() : "";
        redirect.setUrl(paymentProperties.getFrontendResultUrl() + "?invoiceCode=" +
                java.net.URLEncoder.encode(code, java.nio.charset.StandardCharsets.UTF_8) + invoiceId + "&status=" + result.code());
        return redirect;
    }
}
