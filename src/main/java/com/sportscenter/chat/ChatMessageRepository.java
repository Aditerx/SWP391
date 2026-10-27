package com.sportscenter.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    List<ChatMessage> findByConversation_IdAndIdGreaterThanOrderByIdAsc(Integer conversationId, Integer afterId, Pageable pageable);
    List<ChatMessage> findByConversation_IdOrderByIdDesc(Integer conversationId, Pageable pageable);
    Optional<ChatMessage> findByConversation_IdAndSender_IdAndClientMessageId(Integer conversationId, Integer senderId, String clientMessageId);
    long countBySender_IdAndCreatedAtAfter(Integer senderId, LocalDateTime after);
    Optional<ChatMessage> findFirstBySender_IdAndCreatedAtAfterOrderByCreatedAtAsc(Integer senderId, LocalDateTime after);
    @Query("select coalesce(max(m.id),0) from ChatMessage m where m.conversation.id=:conversationId")
    Integer latestId(@Param("conversationId") Integer conversationId);
    @Query("select count(m) from ChatMessage m where m.conversation.id=:conversationId and m.id>:afterId and " +
            "((:forStaff=true and m.senderType='MEMBER') or (:forStaff=false and m.senderType in ('STAFF','AI','SYSTEM')))")
    long countUnread(@Param("conversationId") Integer conversationId, @Param("afterId") Integer afterId,
                     @Param("forStaff") boolean forStaff);
    @Query("select m from ChatMessage m where m.conversation.id=:conversationId order by m.id desc")
    List<ChatMessage> latestMessages(@Param("conversationId") Integer conversationId, Pageable pageable);
}
