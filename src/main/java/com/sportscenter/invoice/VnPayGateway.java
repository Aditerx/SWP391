package com.sportscenter.invoice;

import com.sportscenter.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class VnPayGateway {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final PaymentProperties properties;

    public VnPayGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    public String createPaymentUrl(Invoice invoice, String clientIp) {
        requireConfigured();
        var now = java.time.ZonedDateTime.now(VIETNAM_ZONE);
        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getVnpayTmnCode());
        params.put("vnp_Amount", invoice.getAmount().movePointRight(2).longValueExact() + "");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", invoice.getInvoiceCode());
        params.put("vnp_OrderInfo", (invoice.getSportsClass() != null ? "Thanh toan lop " : "Thanh toan goi ") + invoice.getInvoiceCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.getVnpayReturnUrl());
        params.put("vnp_IpAddr", normalizeIp(clientIp));
        params.put("vnp_CreateDate", VNPAY_DATE.format(now));
        params.put("vnp_ExpireDate", VNPAY_DATE.format(invoice.getExpiresAt().atZone(VIETNAM_ZONE)));

        String hashData = encodeParams(params);
        params.put("vnp_SecureHash", hmacSha512(hashData, properties.getVnpayHashSecret()));
        String query = params.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return properties.getVnpayPayUrl() + "?" + query;
    }

    public boolean verifyCallback(Map<String, String> callbackParams) {
        String suppliedHash = callbackParams.get("vnp_SecureHash");
        if (suppliedHash == null || suppliedHash.isBlank() || properties.getVnpayHashSecret().isBlank()) {
            return false;
        }
        TreeMap<String, String> signedParams = new TreeMap<>();
        callbackParams.forEach((key, value) -> {
            if (key != null && value != null && !key.equals("vnp_SecureHash")
                    && !key.equals("vnp_SecureHashType")) {
                signedParams.put(key, value);
            }
        });
        String expectedHash = hmacSha512(encodeParams(signedParams), properties.getVnpayHashSecret());
        return MessageDigest.isEqual(expectedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                suppliedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    public boolean isValid(Map<String, String> callbackParams) { return verifyCallback(callbackParams); }

    private String encodeParams(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String hmacSha512(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign VNPAY request", exception);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return "127.0.0.1";
        String candidate = clientIp.split(",")[0].trim();
        return candidate.length() > 45 ? "127.0.0.1" : candidate;
    }

    private void requireConfigured() {
        if (properties.getVnpayTmnCode().isBlank() || properties.getVnpayHashSecret().isBlank()) {
            throw new BusinessException("VNPay is not configured: VNPAY_TMN_CODE and VNPAY_HASH_SECRET are required");
        }
    }
}
