package com.sportscenter.user.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

public class MemberSelfProfilePatch {
    private final Map<String, JsonNode> fields = new LinkedHashMap<>();

    @JsonAnySetter
    public void put(String name, JsonNode value) {
        fields.put(name, value);
    }

    public Map<String, JsonNode> fields() {
        return Map.copyOf(fields);
    }
}
