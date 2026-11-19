package com.sportscenter.common.exception;

import com.sportscenter.chat.ChatException;
import com.sportscenter.notification.NotificationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Handler wire mapping only; authenticated business/commit checks live in integration tests. */
class GlobalExceptionHandlerTest {
    MockMvc mvc;
    @BeforeEach void setup(){mvc=MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new GlobalExceptionHandler()).build();}
    @RestController static class Probe {
        @GetMapping("/probe/{kind}") void fail(@PathVariable String kind){
            switch(kind){
                case "plain409" -> throw new BusinessConflictException("NORMAL_VERSION_STALE");
                case "coded409" -> throw new BusinessConflictException("LAST_ADMIN_REQUIRED: Keep administration");
                case "plain400" -> throw new BusinessException("Legacy message");
                case "coded400" -> throw new BusinessException("LEGACY_MEMBERSHIP_RETIRED: Use course registration");
                case "403" -> throw new AccessDeniedException("RESULT_CONTENT_FORBIDDEN: Assigned Coach required");
                case "correction403" -> throw new AccessDeniedException("ATTENDANCE_CORRECTION_FORBIDDEN: Assigned Coach required");
                case "401" -> throw new BadCredentialsException("Private reason must not escape");
                case "404" -> throw new ResourceNotFoundException("Missing resource");
                case "notification404" -> throw new NotificationNotFoundException();
                case "integrity" -> throw new DataIntegrityViolationException("PRIVATE SQL",new SQLException("PRIVATE TABLE","23505"));
                case "hibernate" -> throw new jakarta.persistence.PersistenceException("PRIVATE SQL",new org.hibernate.exception.ConstraintViolationException("PRIVATE TABLE",new SQLException("PRIVATE INDEX","23505"),"PRIVATE CONSTRAINT"));
                case "chat429" -> throw new ChatException(429,"CHAT_RATE_LIMITED","Wait",7);
                case "chat409" -> throw new ChatException(409,"CHAT_CONVERSATION_CLOSED","Closed");
                default -> throw new IllegalStateException("Unrelated failure must not become409");
            }
        }
    }
    @Test void plainConflictMustUse409InBothHttpAndBody()throws Exception {
        mvc.perform(get("/probe/plain409")).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.error").value("Conflict")).andExpect(jsonPath("$.message").value("NORMAL_VERSION_STALE")).andExpect(jsonPath("$.code").doesNotExist());
    }
    @Test void codedConflictKeepsExistingTwoFieldShape()throws Exception {
        mvc.perform(get("/probe/coded409")).andExpect(status().isConflict()).andExpect(jsonPath("$",aMapWithSize(2))).andExpect(jsonPath("$.code").value("LAST_ADMIN_REQUIRED"));
    }
    @Test void businessShapesStayCompatible()throws Exception {
        mvc.perform(get("/probe/plain400")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.code").doesNotExist());
        mvc.perform(get("/probe/coded400")).andExpect(status().isBadRequest()).andExpect(jsonPath("$",aMapWithSize(2))).andExpect(jsonPath("$.code").value("LEGACY_MEMBERSHIP_RETIRED"));
    }
    @Test void permissionShapesStayCompatible()throws Exception {
        mvc.perform(get("/probe/403")).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403)).andExpect(jsonPath("$.code").doesNotExist()).andExpect(jsonPath("$.message",startsWith("RESULT_CONTENT_FORBIDDEN:")));
        mvc.perform(get("/probe/correction403")).andExpect(status().isForbidden()).andExpect(jsonPath("$",aMapWithSize(2))).andExpect(jsonPath("$.code").value("ATTENDANCE_CORRECTION_FORBIDDEN"));
    }
    @Test void authenticationAndNotFoundKeepTheirOwnMappings()throws Exception {
        mvc.perform(get("/probe/401")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.message").value("Invalid email or password"));
        mvc.perform(get("/probe/404")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(get("/probe/notification404")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }
    @Test void translatedIntegrityViolationStaysGeneric409()throws Exception { integrity("integrity"); }
    @Test void nestedHibernateConstraintFromAuditFlushAlsoMapsTo409()throws Exception { integrity("hibernate"); }
    void integrity(String kind)throws Exception {
        mvc.perform(get("/probe/"+kind)).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andExpect(jsonPath("$.message").value("Operation violates a database constraint")).andExpect(content().string(not(containsString("PRIVATE"))));
    }
    @Test void chatCodeAndRetryAfterArePreserved()throws Exception {
        mvc.perform(get("/probe/chat429")).andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","7")).andExpect(jsonPath("$.retryAfterSeconds").value(7)).andExpect(jsonPath("$.code").value("CHAT_RATE_LIMITED"));
        mvc.perform(get("/probe/chat409")).andExpect(status().isConflict()).andExpect(header().doesNotExist("Retry-After")).andExpect(jsonPath("$",aMapWithSize(2)));
    }
    @Test void unrelatedFailuresAreNotDisguisedAsConflicts(){assertThrows(jakarta.servlet.ServletException.class,()->mvc.perform(get("/probe/unknown")));}
}
