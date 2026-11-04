package com.sportscenter.invoice;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PaymentProperties {
    @Value("${payment.expire-minutes:5}")
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

    @Value("${vnpay.query-enabled:false}")
    private boolean vnpayQueryEnabled;
    @Value("${vnpay.query-settlement-enabled:false}")
    private boolean vnpayQuerySettlementEnabled;
    @Value("${vnpay.query-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String vnpayQueryUrl;
    @Value("${vnpay.query-ip-address:}")
    private String vnpayQueryIpAddress;
    @Value("${vnpay.query-connect-timeout-seconds:3}")
    private int vnpayQueryConnectTimeoutSeconds = 3;
    @Value("${vnpay.query-request-timeout-seconds:5}")
    private int vnpayQueryRequestTimeoutSeconds = 5;
}
