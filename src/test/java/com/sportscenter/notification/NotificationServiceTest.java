package com.sportscenter.notification;

import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository repository;
    @Mock NotificationRecipientRepository recipientRepository;
    @Mock UserRepository userRepository;
    @Mock Clock clock;
    @InjectMocks NotificationService service;

    @BeforeEach
    void setClock() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-22T02:15:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @Test
    void sendPersistsTypedNotificationForRecipient() {
        User user = new User();
        user.setId(14);
        when(userRepository.getReferenceById(14)).thenReturn(user);
        when(repository.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));

        service.send(14, "Class update", "You were promoted from the waitlist", "Schedule");

        verify(repository).save(argThat(n -> n.getUser() == user && n.getTitle().equals("Class update")
                && n.getContent().contains("promoted") && n.getType().equals("Schedule") && !n.isRead()));
    }

    @Test
    void createPersistsEventAndRecipient() {
        User recipient = new User(); recipient.setId(14);
        Notification notification = new Notification(); notification.setId(90);
        notification.setCreatedAt(LocalDateTime.of(2026, 10, 22, 9, 15));
        when(repository.insertEventIfAbsent(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any()))
                .thenReturn(1);
        when(repository.findByDedupeKey("SESSION_REMINDER:90:14:24h")).thenReturn(Optional.of(notification));
        when(userRepository.getReferenceById(14)).thenReturn(recipient);

        boolean created = service.create(new NotificationEvent(List.of(14), NotificationType.SESSION_REMINDER,
                "Nhắc lịch", "Nội dung", "SESSION", 90, "SESSION_REMINDER:90:14:24h"));

        org.junit.jupiter.api.Assertions.assertTrue(created);
        verify(recipientRepository).saveAll(argThat(rows -> ((Iterable<?>) rows).iterator().hasNext()));
    }

    @Test
    void createSkipsDuplicateDedupeKey() {
        when(repository.insertEventIfAbsent(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString(), any()))
                .thenReturn(0);

        boolean created = service.create(new NotificationEvent(List.of(14), NotificationType.SESSION_REMINDER,
                "Nhắc lịch", "Nội dung", "SESSION", 90, "SESSION_REMINDER:90:14:24h"));

        org.junit.jupiter.api.Assertions.assertFalse(created);
        verifyNoInteractions(recipientRepository);
    }

    @Test
    void listMineUsesFiltersAndPageableAndUnreadCountIsScoped() {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(repository.findMine(14, true, "PAYMENT_SUCCESS", pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));
        when(repository.countUnreadForUser(14)).thenReturn(3L);

        service.listMine(14, true, NotificationType.PAYMENT_SUCCESS, pageable);

        org.junit.jupiter.api.Assertions.assertEquals(3L, service.countUnread(14));
        verify(repository).findMine(14, true, "PAYMENT_SUCCESS", pageable);
        verify(repository).countUnreadForUser(14);
    }

    @Test
    void markReadUpdatesRecipientAndMarkAllReadUpdatesRecipientRows() {
        when(repository.findRecipientRead(90, 14)).thenReturn(Optional.of(new NotificationRepository.RecipientReadRow() {
            public Integer getNotificationId() { return 90; }
            public Integer getUserId() { return 14; }
            public boolean getIsRead() { return false; }
        }));
        service.markRead(14, 90);
        service.markAllRead(14);

        verify(repository).markRecipientRead(eq(90), eq(14), any(LocalDateTime.class));
        verify(repository).markAllRecipientsRead(eq(14), any(LocalDateTime.class));
        verify(repository).markAllDirectRead(14);
    }

    @Test
    void markReadReturnsNotFoundForAnotherUsersNotification() {
        when(repository.findRecipientRead(90, 14)).thenReturn(Optional.empty());
        when(repository.isOwnedByUser(90, 14)).thenReturn(false);

        assertThrows(NotificationNotFoundException.class, () -> service.markRead(14, 90));
        verify(repository, never()).markDirectRead(90, 14);
    }

    @Test
    void sendRunsInRequiresNewTransaction() throws Exception {
        Method send = NotificationService.class.getMethod("send", Integer.class, String.class, String.class, String.class);
        assertEquals(Propagation.REQUIRES_NEW, send.getAnnotation(Transactional.class).propagation());
    }

    @Test
    void createRunsInRequiresNewTransaction() throws Exception {
        Method create = NotificationService.class.getMethod("create", NotificationEvent.class);
        assertEquals(Propagation.REQUIRES_NEW, create.getAnnotation(Transactional.class).propagation());
    }
}
