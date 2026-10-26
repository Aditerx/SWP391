package com.sportscenter.chat;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ChatReadStateRepository extends JpaRepository<ChatReadState, ChatReadStateId> {
    java.util.Optional<ChatReadState> findByConversation_IdAndUser_Id(Integer conversationId, Integer userId);
}
