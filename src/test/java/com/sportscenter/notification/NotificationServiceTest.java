package com.sportscenter.notification;

import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository repository;
    @Mock UserRepository userRepository;
    @InjectMocks NotificationService service;

    @Test
    void sendPersistsTypedNotificationForRecipient() {
        User user = new User();
        when(userRepository.getReferenceById(14)).thenReturn(user);
        when(repository.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));

        service.send(14, "Class update", "You were promoted from the waitlist", "Schedule");

        verify(repository).save(argThat(n -> n.getUser() == user && n.getTitle().equals("Class update")
                && n.getContent().contains("promoted") && n.getType().equals("Schedule") && !n.isRead()));
    }

    @Test
    void sendRunsInRequiresNewTransaction() throws Exception {
        Method send = NotificationService.class.getMethod("send", Integer.class, String.class, String.class, String.class);
        assertEquals(Propagation.REQUIRES_NEW, send.getAnnotation(Transactional.class).propagation());
    }
}
