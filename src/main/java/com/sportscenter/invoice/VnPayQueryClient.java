package com.sportscenter.invoice;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class VnPayQueryClient {
    private static final List<String> REQUEST_FIELDS = List.of("vnp_RequestId", "vnp_Version", "vnp_Command",
            "vnp_TmnCode", "vnp_TxnRef", "vnp_TransactionDate", "vnp_CreateDate", "vnp_IpAddr", "vnp_OrderInfo");
    private static final List<String> RESPONSE_FIELDS = List.of("vnp_ResponseId", "vnp_Command", "vnp_ResponseCode",
            "vnp_Message", "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate",
            "vnp_TransactionNo", "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo",
            "vnp_PromotionCode", "vnp_PromotionAmount");
    private final PaymentProperties properties;
    private final Clock clock;
    private final ObjectMapper mapper = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public VnPayQueryClient(PaymentProperties properties, Clock clock) { this.properties = properties; this.clock = clock; }

    public VerifiedVnPayQuery query(VnPayPaymentAttempt.Snapshot attempt) {
        try {
            if (properties.getVnpayHashSecret().isBlank() || !attempt.tmnCode().equals(properties.getVnpayTmnCode()))
                throw new VnPayQueryException("MERCHANT_NOT_CONFIGURED");
            URI endpoint = URI.create(properties.getVnpayQueryUrl());
            // HTTP is permitted only for local test servers. Never follow a redirect with signed data.
            boolean local = "127.0.0.1".equals(endpoint.getHost()) || "localhost".equals(endpoint.getHost());
            if ((!"https".equals(endpoint.getScheme()) && !(local && "http".equals(endpoint.getScheme())))
                    || endpoint.getUserInfo() != null || endpoint.getQuery() != null || endpoint.getFragment() != null)
                throw new VnPayQueryException("INVALID_QUERY_ENDPOINT");
            Map<String, String> request = new LinkedHashMap<>();
            request.put("vnp_RequestId", UUID.randomUUID().toString().replace("-", ""));
            request.put("vnp_Version", "2.1.0"); request.put("vnp_Command", "querydr");
            request.put("vnp_TmnCode", attempt.tmnCode()); request.put("vnp_TxnRef", attempt.txnRef());
            request.put("vnp_TransactionDate", attempt.transactionDate());
            request.put("vnp_CreateDate", DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    .format(ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))));
            String ip = properties.getVnpayQueryIpAddress();
            if (ip == null || ip.isBlank()) ip = InetAddress.getLocalHost().getHostAddress();
            if (ip.length() > 45 || !ip.matches("[0-9a-fA-F:.]+")) throw new VnPayQueryException("INVALID_SERVER_IP");
            request.put("vnp_IpAddr", ip);
            request.put("vnp_OrderInfo", "Query " + attempt.txnRef());
            request.put("vnp_SecureHash", sign(request, REQUEST_FIELDS));
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint).timeout(java.time.Duration.ofSeconds(5))
                    .header("Content-Type", "application/json").header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request))).build();
            HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200 || response.body().length() > 65536)
                throw new VnPayQueryException("PROVIDER_HTTP_ERROR");
            return verify(response.body(), attempt);
        } catch (VnPayQueryException ex) { throw ex;
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new VnPayQueryException("QUERY_INTERRUPTED");
        } catch (Exception ex) { throw new VnPayQueryException("QUERY_UNAVAILABLE"); }
    }

    VerifiedVnPayQuery verify(String body, VnPayPaymentAttempt.Snapshot attempt) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root == null || !root.isObject()) throw new VnPayQueryException("INVALID_RESPONSE");
            Map<String, String> fields = new LinkedHashMap<>();
            for (String key : RESPONSE_FIELDS) {
                JsonNode value = root.get(key);
                if (value == null || value.isNull()) fields.put(key, "");
                else if (value.isTextual() || value.isIntegralNumber()) fields.put(key, value.asText());
                else throw new VnPayQueryException("INVALID_RESPONSE_FIELD");
            }
            JsonNode hashNode = root.get("vnp_SecureHash");
            String supplied = hashNode != null && hashNode.isTextual() ? hashNode.asText() : "";
            if (!supplied.matches("[a-fA-F0-9]{128}") || !MessageDigest.isEqual(
                    HexFormat.of().parseHex(sign(fields, RESPONSE_FIELDS)), HexFormat.of().parseHex(supplied)))
                throw new VnPayQueryException("INVALID_RESPONSE_SIGNATURE");
            if (!attempt.tmnCode().equals(fields.get("vnp_TmnCode")) || !attempt.txnRef().equals(fields.get("vnp_TxnRef"))
                    || fields.get("vnp_ResponseId").isBlank() || !fields.get("vnp_ResponseCode").matches("[0-9]{2}")
                    || !"querydr".equals(fields.get("vnp_Command")))
                throw new VnPayQueryException("RESPONSE_IDENTITY_MISMATCH");
            if (!"00".equals(fields.get("vnp_ResponseCode")))
                return new VerifiedVnPayQuery(attempt.tmnCode(), attempt.txnRef(), attempt.amountMinor(),
                        fields.get("vnp_ResponseCode"), "", "", fields.get("vnp_ResponseId"));
            if (!fields.get("vnp_Amount").matches("[0-9]{1,12}")
                    || Long.parseLong(fields.get("vnp_Amount")) != attempt.amountMinor()
                    || !"01".equals(fields.get("vnp_TransactionType"))
                    || !fields.get("vnp_TransactionStatus").matches("[0-9]{2}")
                    || !fields.get("vnp_TransactionNo").matches("[0-9]{1,15}")
                    || ("00".equals(fields.get("vnp_TransactionStatus")) && Long.parseLong(fields.get("vnp_TransactionNo")) == 0))
                throw new VnPayQueryException("RESPONSE_PAYMENT_MISMATCH");
            return new VerifiedVnPayQuery(attempt.tmnCode(), attempt.txnRef(), attempt.amountMinor(),
                    fields.get("vnp_ResponseCode"), fields.get("vnp_TransactionStatus"),
                    fields.get("vnp_TransactionNo"), fields.get("vnp_ResponseId"));
        } catch (VnPayQueryException ex) { throw ex;
        } catch (Exception ex) { throw new VnPayQueryException("INVALID_RESPONSE"); }
    }

    private String sign(Map<String, String> fields, List<String> order) throws Exception {
        String data = String.join("|", order.stream().map(key -> fields.getOrDefault(key, "")).toList());
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(properties.getVnpayHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
