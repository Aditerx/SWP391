package com.sportscenter.invoice;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PaymentProperties {
    @Value("${payment.expire-minutes:15}")
    private int expireMinutes;

    @Value("${vnpay.tmn-code:}")
    private String vnpayTmnCode;

    @Value("${vnpay.hash-secret:}")
    private String vnpayHashSecret;

    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpayPayUrl;

    @Value("${vnpay.return-url:http://localhost:8080/api/payments/vnpay/return}")
    private String vnpayReturnUrl;

    @Value("${payment.frontend-result-url:http://localhost:5173/payment-result}")
    private String frontendResultUrl;
}
