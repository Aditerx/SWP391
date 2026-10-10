package com.sportscenter.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpirationJob {
    private final InvoiceService invoiceService;

    @Scheduled(fixedDelay = 60_000L)
    public void expirePendingPayments() {
        int count = 0;
        for (Integer invoiceId : invoiceService.findExpiredPendingInvoiceIds()) {
            try {
                if (invoiceService.expireInvoice(invoiceId)) count++;
            } catch (RuntimeException exception) {
                log.error("Could not expire invoice {}", invoiceId, exception);
            }
        }
        if (count > 0) log.info("Expired {} pending payment invoices", count);
    }
}
