package com.sportscenter.holiday;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {
    private final HolidayService holidayService;
    private final HolidayImpactService holidayImpactService;
    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')")
    public HolidayImpactService.Preview preview(@RequestBody HolidayImpactService.Change change,org.springframework.security.core.Authentication auth){return holidayImpactService.preview(change,auth);}

    @GetMapping
    public ResponseEntity<List<HolidayResponse>> findAll() {
        return ResponseEntity.ok(holidayService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<HolidayResponse>> findActive() {
        return ResponseEntity.ok(holidayService.findActive());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<HolidayResponse> toggleActive(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, Boolean> body,
            @RequestHeader(value="X-Preview-Token",required=false) String token,org.springframework.security.core.Authentication auth) {
        Boolean active = body != null ? body.get("active") : null;
        return ResponseEntity.ok(holidayImpactService.confirm(new HolidayImpactService.Change(id,null,null,null,active),token,auth));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CENTERMANAGER','ADMIN') and hasAuthority('MANAGE_CLASSES')")
    public ResponseEntity<HolidayResponse> create(@Valid @RequestBody HolidayRequest request,
            @RequestHeader(value="X-Preview-Token",required=false) String token,org.springframework.security.core.Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayImpactService.confirm(new HolidayImpactService.Change(null,request.holidayDate(),request.holidayName(),request.tag(),request.active()),token,auth));
    }
}

