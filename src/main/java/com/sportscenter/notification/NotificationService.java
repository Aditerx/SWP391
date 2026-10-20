package com.sportscenter.notification;

import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Integer userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUser(userRepository.getReferenceById(userId));
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        repository.save(notification);
    }
}
