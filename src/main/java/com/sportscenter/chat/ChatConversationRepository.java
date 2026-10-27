package com.sportscenter.chat;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Integer> {
    Optional<ChatConversation> findFirstByMember_IdAndStatusOrderByIdDesc(Integer memberId, ChatConversationStatus status);
    java.util.List<ChatConversation> findByMember_Id(Integer memberId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from ChatConversation c where c.id=:id")
    Optional<ChatConversation> findByIdForUpdate(@Param("id") Integer id);
    @Query("select c from ChatConversation c where (:status is null or c.status=:status) " +
            "and (:assigned is null or (:assigned=true and c.assignedStaff.id=:staffId) or (:assigned=false and c.assignedStaff is null)) " +
            "and (:unreadOnly=false or exists (select m.id from ChatMessage m where m.conversation=c " +
            "and m.senderType=com.sportscenter.chat.ChatSenderType.MEMBER and m.id>coalesce(" +
            "(select rs.lastReadMessageId from ChatReadState rs where rs.conversation=c and rs.user.id=:staffId),0))) " +
            "order by coalesce(c.lastMessageAt,c.createdAt) desc, c.id desc")
    Page<ChatConversation> inbox(@Param("status") ChatConversationStatus status, @Param("assigned") Boolean assigned,
                                @Param("staffId") Integer staffId, @Param("unreadOnly") boolean unreadOnly, Pageable pageable);
    @Query("select c from ChatConversation c where c.id=:id and c.member.id=:memberId")
    Optional<ChatConversation> findMemberConversation(@Param("id") Integer id, @Param("memberId") Integer memberId);
}
