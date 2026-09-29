package com.sportscenter.user;

import com.sportscenter.user.dto.StaffRequest;
import com.sportscenter.user.dto.StaffResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {
    private final StaffService staffService;

    @GetMapping
    public List<StaffResponse> getAllStaff() {
        return staffService.findAllStaff();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse createStaff(@Valid @RequestBody StaffRequest request) {
        return staffService.createStaff(request);
    }

    @PutMapping("/{id}")
    public StaffResponse updateStaff(@PathVariable Integer id, @Valid @RequestBody StaffRequest request) {
        return staffService.updateStaff(id, request);
    }

    @PatchMapping("/{id}/status")
    public StaffResponse updateStaffStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        return staffService.updateStaffStatus(id, status);
    }
}
