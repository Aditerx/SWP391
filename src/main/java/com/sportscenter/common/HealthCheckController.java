package com.sportscenter.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Endpoint for keep-alive and uptime monitoring")
public class HealthCheckController {
    private final RuntimeIdentity identity;

    public HealthCheckController(RuntimeIdentity identity) { this.identity = identity; }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Operation(summary = "Ping endpoint for keep-alive services")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new LinkedHashMap<>(Map.of("status", "UP", "message", "pong"));
        response.putAll(identity.values());
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(response);
    }
}
