package com.sportscenter.user.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperties;
import io.swagger.v3.oas.annotations.media.SchemaProperty;

import java.util.LinkedHashMap;
import java.util.Map;

@Schema(description = "Absent fields are unchanged; nullable fields can be cleared. Unknown keys are ignored.", additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
@SchemaProperties({
        @SchemaProperty(name="name", schema=@Schema(type="string",maxLength=100,description="Optional; if supplied must be non-null and non-blank")),
        @SchemaProperty(name="phone", schema=@Schema(type="string",nullable=true,maxLength=20)),
        @SchemaProperty(name="address", schema=@Schema(type="string",nullable=true,maxLength=255)),
        @SchemaProperty(name="dateOfBirth", schema=@Schema(type="string",format="date",nullable=true,description="Must not be later than today in Asia/Ho_Chi_Minh")),
        @SchemaProperty(name="gender", schema=@Schema(type="string",nullable=true,allowableValues={"Male","Female","Other"})),
        @SchemaProperty(name="goal", schema=@Schema(type="string",nullable=true,maxLength=255)),
        @SchemaProperty(name="healthNote", schema=@Schema(type="string",nullable=true))
})
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
