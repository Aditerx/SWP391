package com.sportscenter.training;
public record CheckinCodeResponse(String payload, int expiresInSeconds, java.time.OffsetDateTime expiresAt,
                                  java.time.OffsetDateTime serverTime) {
    @Override public String toString() { return "CheckinCodeResponse[payload=[redacted], expiresInSeconds="+expiresInSeconds+", expiresAt="+expiresAt+", serverTime="+serverTime+"]"; }
}
