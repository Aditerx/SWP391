package com.sportscenter.user;

import com.sportscenter.user.dto.UserRequest;
import com.sportscenter.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_USERS') or hasAuthority('MANAGE_RBAC')")
    public List<UserResponse> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search
    ) {
        return userService.findAllUsers(role, search);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS') or hasAuthority('MANAGE_RBAC')")
    public UserResponse getUserById(@PathVariable Integer id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public UserResponse updateUser(@PathVariable Integer id, @Valid @RequestBody UserRequest request) {
        return userService.updateUser(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public UserResponse updateUserStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        return userService.updateUserStatus(id, status);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('MANAGE_USERS') or hasAuthority('MANAGE_RBAC')")
    public UserResponse updateUserRole(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer roleId = body != null ? body.get("roleId") : null;
        return userService.updateUserRole(id, roleId);
    }
}
