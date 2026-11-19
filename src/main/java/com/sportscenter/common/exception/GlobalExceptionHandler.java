package com.sportscenter.common.exception;

import com.sportscenter.feedback.ClassFeedbackConflictException;
import com.sportscenter.auth.GoogleAuthException;
import com.sportscenter.chat.ChatException;
import com.sportscenter.invoice.InvoiceConflictException;
import com.sportscenter.notification.NotificationNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessConflictException.class)
    ResponseEntity<Map<String, Object>> conflict(BusinessConflictException ex) {
        return business(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ChatException.class)
    ResponseEntity<Map<String, Object>> chat(ChatException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatus());
        if (ex.getRetryAfterSeconds() != null) {
            body.put("retryAfterSeconds", ex.getRetryAfterSeconds());
            response.header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        }
        return response.body(body);
    }

    @ExceptionHandler(GoogleAuthException.class)
    ResponseEntity<Map<String, Object>> googleAuth(GoogleAuthException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    ResponseEntity<Map<String, Object>> notificationNotFound(NotificationNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "NOTIFICATION_NOT_FOUND");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Map<String, Object>> business(BusinessException ex) {
        return business(HttpStatus.BAD_REQUEST, ex);
    }

    private ResponseEntity<Map<String, Object>> business(HttpStatus status, BusinessException ex) {
        int separator = ex.getMessage() == null ? -1 : ex.getMessage().indexOf(":");
        if (separator > 0 && ex.getMessage().substring(0, separator).matches("[A-Z_]+")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", ex.getMessage().substring(0, separator));
            body.put("message", ex.getMessage().substring(separator + 1).trim());
            return ResponseEntity.status(status).body(body);
        }
        return error(status, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Map<String, Object>> authentication(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException ex, jakarta.servlet.http.HttpServletRequest request) {
        String actionCode=null;
        if ("POST".equals(request.getMethod())) {
            String path=request.getRequestURI();
            if (path.matches("/api/classes/[^/]+/confirm-opening")) actionCode="OPENING_CONFIRMATION_FORBIDDEN";
            else if (path.matches("/api/classes/[^/]+/enroll") || path.equals("/api/enrollments")) actionCode="REGISTRATION_FORBIDDEN";
            else if (path.matches("/api/advanced-registrations/[^/]+/order")) actionCode="REGISTRATION_OWNER_REQUIRED";
            else if (path.matches("/api/sessions/[^/]+/cancel-private")) actionCode="CANCELLATION_FORBIDDEN";
            else if (path.matches("/api/training-results(?:/bulk)?") || path.matches("/api/attendances?/scan")) actionCode="ATTENDANCE_FORBIDDEN";
        }
        if (ex.getMessage() != null && (ex.getMessage().startsWith("ATTENDANCE_CORRECTION_") || actionCode!=null)) {
            int separator = ex.getMessage().indexOf(':');
            if (separator > 0 && ex.getMessage().substring(0,separator).matches("[A-Z_]+")) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "code", ex.getMessage().substring(0, separator), "message", ex.getMessage().substring(separator + 1).trim()));
        }
        if (actionCode!=null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code",actionCode,"message",ex.getMessage()==null?"Access denied":ex.getMessage()));
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class})
    ResponseEntity<Map<String, Object>> integrity(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, "Operation violates a database constraint");
    }

    @ExceptionHandler(ClassFeedbackConflictException.class)
    ResponseEntity<Map<String, Object>> classFeedbackConflict(ClassFeedbackConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvoiceConflictException.class)
    ResponseEntity<Map<String, Object>> invoiceConflict(InvoiceConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
