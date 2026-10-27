package com.sportscenter.notification;

import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Thông báo trong ứng dụng của người dùng đang đăng nhập")
public class NotificationController {
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @Operation(summary = "Danh sách thông báo của tôi", description = "Trả về trang thông báo mới nhất trước của user đang đăng nhập.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Danh sách thông báo"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")})
    public ResponseEntity<Page<NotificationResponse>> listMine(
            @Parameter(description = "Chỉ lấy thông báo chưa đọc") @RequestParam(defaultValue = "false") boolean unreadOnly,
            @Parameter(description = "Lọc theo loại thông báo") @RequestParam(required = false) NotificationType type,
            @Parameter(description = "Tham số phân trang")
            Pageable pageable,
            Authentication authentication) {
        User user = currentUser(authentication);
        int size = Math.max(1, Math.min(pageable.getPageSize(), 100));
        Pageable safePageable = PageRequest.of(Math.max(0, pageable.getPageNumber()), size);
        return ResponseEntity.ok(notificationService.listMine(user.getId(), unreadOnly, type, safePageable));
    }

    @GetMapping("/me/unread-count")
    @Operation(summary = "Đếm thông báo chưa đọc của tôi")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Số lượng chưa đọc"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")})
    public ResponseEntity<UnreadCountResponse> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.countUnread(currentUser(authentication).getId())));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu thông báo của tôi đã đọc")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Đã cập nhật"),
            @ApiResponse(responseCode = "404", description = "NOTIFICATION_NOT_FOUND")})
    public ResponseEntity<Void> markRead(@PathVariable Integer id, Authentication authentication) {
        notificationService.markRead(currentUser(authentication).getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/read-all")
    @Operation(summary = "Đánh dấu toàn bộ thông báo của tôi đã đọc")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Đã cập nhật"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")})
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        notificationService.markAllRead(currentUser(authentication).getId());
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null) throw new ResourceNotFoundException("Authenticated user not found");
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
