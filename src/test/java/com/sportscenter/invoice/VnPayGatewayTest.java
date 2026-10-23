package com.sportscenter.invoice;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;

class VnPayGatewayTest {
    PaymentProperties properties() {
        var p = new PaymentProperties();
        ReflectionTestUtils.setField(p,"vnpayTmnCode","TESTMERCHANT");
        ReflectionTestUtils.setField(p,"vnpayHashSecret","test-only-secret");
        ReflectionTestUtils.setField(p,"vnpayPayUrl","https://sandbox.test/pay");
        ReflectionTestUtils.setField(p,"vnpayReturnUrl","https://app.test/return");
        return p;
    }
    Clock clock = Clock.fixed(Instant.parse("2026-10-23T01:00:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"));
    Map<String,String> signedRequest(VnPayGateway gateway) {
        var invoice = new Invoice(); invoice.setInvoiceCode("TEST-1"); invoice.setAmount(java.math.BigDecimal.TEN); invoice.setExpiresAt(LocalDateTime.now(clock).plusMinutes(5));
        var params = new TreeMap<String,String>();
        for (var part : URI.create(gateway.createPaymentUrl(invoice,"127.0.0.1")).getRawQuery().split("&")) {
            String[] item = part.split("=",2); params.put(URLDecoder.decode(item[0],StandardCharsets.UTF_8),URLDecoder.decode(item[1],StandardCharsets.UTF_8));
        }
        return params;
    }
    @Test void paymentRequestUsesInjectedVietnamClockAndValidSignature() {
        var gateway = new VnPayGateway(properties(),clock); var params = signedRequest(gateway);
        assertEquals("20261023080000",params.get("vnp_CreateDate"));
        assertEquals("20261023080500",params.get("vnp_ExpireDate"));
        assertTrue(gateway.verifyCallback(params));
    }
    @Test void signatureAloneDoesNotAuthorizeAnotherMerchant() throws Exception {
        var gateway = new VnPayGateway(properties(),clock); var params = signedRequest(gateway);
        params.put("vnp_TmnCode","ANOTHER-MERCHANT"); params.remove("vnp_SecureHash");
        String data = params.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(),StandardCharsets.US_ASCII)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.US_ASCII)).collect(java.util.stream.Collectors.joining("&"));
        Mac mac = Mac.getInstance("HmacSHA512"); mac.init(new SecretKeySpec("test-only-secret".getBytes(StandardCharsets.UTF_8),"HmacSHA512"));
        params.put("vnp_SecureHash",HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8))));
        assertFalse(gateway.verifyCallback(params));
    }
    @Test void tamperedAmountInvalidatesSignature() {
        var gateway = new VnPayGateway(properties(),clock); var params = signedRequest(gateway); params.put("vnp_Amount","1"); assertFalse(gateway.verifyCallback(params));
    }
}
