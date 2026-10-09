package com.sportscenter.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Endpoint for keep-alive and uptime monitoring")
public class HealthCheckController {

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Operation(summary = "Ping endpoint for keep-alive services")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("status", "UP", "message", "pong"));
    }
}
