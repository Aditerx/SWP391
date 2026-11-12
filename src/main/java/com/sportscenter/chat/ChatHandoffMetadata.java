package com.sportscenter.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;

final class ChatHandoffMetadata {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ChatHandoffMetadata() { }

    static boolean suggested(String metadata) {
        try { return metadata != null && JSON.readTree(metadata).path("handoffSuggested").asBoolean(false); }
        catch (Exception ignored) { return false; }
    }

    static String reasonCode(String metadata) {
        try {
            JsonNode value = metadata == null ? null : JSON.readTree(metadata).get("reasonCode");
            return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
        } catch (Exception ignored) { return null; }
    }

    static String withSuggestion(String metadata, String reasonCode) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        try {
            JsonNode root = metadata == null ? null : JSON.readTree(metadata);
            if (root != null && root.isObject()) root.fields().forEachRemaining(entry -> values.put(entry.getKey(), JSON.convertValue(entry.getValue(), Object.class)));
        } catch (Exception ignored) { }
        values.put("handoffSuggested", true);
        values.put("reasonCode", reasonCode);
        try { return JSON.writeValueAsString(values); }
        catch (Exception ignored) { return "{\"handoffSuggested\":true,\"reasonCode\":\"AI_HANDOFF_SUGGESTED\"}"; }
    }
}
