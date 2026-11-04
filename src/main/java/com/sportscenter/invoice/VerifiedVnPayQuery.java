package com.sportscenter.invoice;

/** Constructed from a verified server-to-server response, never bound from a client request. */
public record VerifiedVnPayQuery(String tmnCode, String txnRef, long amountMinor,
        String responseCode, String transactionStatus, String transactionNo, String responseId) {
    public boolean successful() { return "00".equals(responseCode) && "00".equals(transactionStatus); }
}
