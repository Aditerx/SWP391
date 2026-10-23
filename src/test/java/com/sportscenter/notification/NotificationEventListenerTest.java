package com.sportscenter.notification;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class NotificationEventListenerTest {
    @Test
    void notificationFailureIsSwallowedAfterBusinessEventDispatch() {
        NotificationService service = mock(NotificationService.class);
        NotificationEventListener listener = new NotificationEventListener(service);
        NotificationEvent event = new NotificationEvent(List.of(1), NotificationType.PAYMENT_SUCCESS,
                "Đã thanh toán", "Nội dung", "INVOICE", 2, "PAYMENT_SUCCESS:2");
        when(service.create(event)).thenThrow(new IllegalStateException("storage unavailable"));

        assertDoesNotThrow(() -> listener.onNotificationEvent(event));
        verify(service).create(event);
    }

    @Test
    void listenerRunsAfterCommit() throws Exception {
        Method method = NotificationEventListener.class.getMethod("onNotificationEvent", NotificationEvent.class);
        assertEquals(TransactionPhase.AFTER_COMMIT, method.getAnnotation(TransactionalEventListener.class).phase());
    }
}
