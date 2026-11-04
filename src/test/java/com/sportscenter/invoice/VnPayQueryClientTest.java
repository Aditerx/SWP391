package com.sportscenter.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VnPayQueryClientTest {
    static final String SECRET = "test-query-secret";
    static final List<String> RESPONSE_ORDER = List.of("vnp_ResponseId", "vnp_Command", "vnp_ResponseCode",
            "vnp_Message", "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate",
            "vnp_TransactionNo", "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo",
            "vnp_PromotionCode", "vnp_PromotionAmount");
    final VnPayPaymentAttempt.Snapshot attempt = new VnPayPaymentAttempt.Snapshot(1L,"TEST0001","INV-Q-1","20261104110340",90000000);
    final Clock clock = Clock.fixed(Instant.parse("2026-11-04T04:04:30Z"), ZoneId.of("UTC"));
    PaymentProperties properties() {
        var p = new PaymentProperties();
        ReflectionTestUtils.setField(p,"vnpayTmnCode","TEST0001");
        ReflectionTestUtils.setField(p,"vnpayHashSecret",SECRET);
        ReflectionTestUtils.setField(p,"vnpayQueryIpAddress","127.0.0.1");
        return p;
    }
    static Map<String,String> response(String ref, String amount, String number) {
        Map<String,String> p = new LinkedHashMap<>();
        p.put("vnp_ResponseId","R1");p.put("vnp_Command","querydr");p.put("vnp_ResponseCode","00");p.put("vnp_Message","OK");
        p.put("vnp_TmnCode","TEST0001");p.put("vnp_TxnRef",ref);p.put("vnp_Amount",amount);p.put("vnp_BankCode","NCB");
        p.put("vnp_PayDate","20261104110400");p.put("vnp_TransactionNo",number);p.put("vnp_TransactionType","01");
        p.put("vnp_TransactionStatus","00");p.put("vnp_OrderInfo","Query "+ref);return p;
    }
    static String hmac(String data) throws Exception {
        Mac mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
    static Map<String,String> signed(Map<String,String> fields) throws Exception {
        fields.put("vnp_SecureHash",hmac(String.join("|",RESPONSE_ORDER.stream().map(k->fields.getOrDefault(k,"")).toList())));return fields;
    }
    String json(Map<String,String> fields) throws Exception { return new ObjectMapper().writeValueAsString(fields); }

    @Test void acceptsIndependentGoldenHmacIncludingEmptyPromotionSlots() throws Exception {
        Map<String,String> p=response("INV-Q-1","90000000","123456");
        // Independently calculated with Python's hashlib/hmac, not the production signer.
        p.put("vnp_SecureHash","c58cf23edec86ec20cecd1a1a1b0d7c1836e7922d00aa514fdba7d31b8d3c3e92da4158db445de59d29bd4dac731851a52f72b22cb2988dd54a8c26cea7ea178");
        assertTrue(new VnPayQueryClient(properties(),clock).verify(json(p),attempt).successful());
    }
    @Test void rejectsTamperingMissingSignatureAndDuplicateJson() throws Exception {
        var client=new VnPayQueryClient(properties(),clock);var p=signed(response("INV-Q-1","90000000","123456"));
        p.put("vnp_Amount","90000001");assertThrows(VnPayQueryException.class,()->client.verify(json(p),attempt));
        p.remove("vnp_SecureHash");assertThrows(VnPayQueryException.class,()->client.verify(json(p),attempt));
        String duplicate=json(signed(response("INV-Q-1","90000000","123456"))).replaceFirst("\\{","{\"vnp_Amount\":\"1\",");
        assertThrows(VnPayQueryException.class,()->client.verify(duplicate,attempt));
    }
    @Test void validSignatureDoesNotAuthorizeWrongMerchantReferenceAmountOrRefund() throws Exception {
        var client=new VnPayQueryClient(properties(),clock);
        for(var change:List.of(Map.entry("vnp_TmnCode","OTHER001"),Map.entry("vnp_TxnRef","INV-OTHER"),
                Map.entry("vnp_Amount","89999999"),Map.entry("vnp_TransactionType","02"),Map.entry("vnp_Command",""))) {
            var p=response("INV-Q-1","90000000","123456");p.put(change.getKey(),change.getValue());signed(p);
            assertThrows(VnPayQueryException.class,()->client.verify(json(p),attempt),change.getKey());
        }
    }
    @Test void querySuccessDoesNotMeanPaymentSuccessAndOptionalPromotionValuesAreSigned() throws Exception {
        var p=response("INV-Q-1","90000000","0");p.put("vnp_TransactionStatus","01");
        p.put("vnp_Message","Chua hoan tat");p.put("vnp_PromotionCode","TEST");p.put("vnp_PromotionAmount","0");signed(p);
        var client=new VnPayQueryClient(properties(),clock);assertFalse(client.verify(json(p),attempt).successful());
        p.put("vnp_PromotionAmount","");assertThrows(VnPayQueryException.class,()->client.verify(json(p),attempt));
    }
    @Test void sendsExactOriginalPayDateAndProtocolSpecificRequestHmacOverHttp() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        server.createContext("/query",exchange->{
            try {
                assertEquals("POST",exchange.getRequestMethod());
                @SuppressWarnings("unchecked") Map<String,String> req=new ObjectMapper().readValue(exchange.getRequestBody(),Map.class);
                assertEquals("20261104110340",req.get("vnp_TransactionDate"));assertEquals("20261104110430",req.get("vnp_CreateDate"));
                assertEquals("querydr",req.get("vnp_Command"));assertEquals(32,req.get("vnp_RequestId").length());
                String data=req.get("vnp_RequestId")+"|2.1.0|querydr|TEST0001|INV-Q-1|20261104110340|20261104110430|127.0.0.1|Query INV-Q-1";
                assertEquals(hmac(data),req.get("vnp_SecureHash"));assertFalse(req.containsValue(SECRET));
                byte[] body=json(signed(response("INV-Q-1","90000000","123456"))).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);
            } catch(Throwable ex) {error.set(ex);exchange.sendResponseHeaders(500,-1);} finally {exchange.close();}
        });server.start();
        try {
            var p=properties();ReflectionTestUtils.setField(p,"vnpayQueryUrl","http://127.0.0.1:"+server.getAddress().getPort()+"/query");
            assertTrue(new VnPayQueryClient(p,clock).query(attempt).successful());assertNull(error.get());
        } finally {server.stop(0);}
    }
}
