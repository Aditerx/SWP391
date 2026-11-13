package com.sportscenter.session;

import java.time.*;

public record PrivateSessionProposalResponse(Integer id,Integer subscriptionId,Integer memberId,Integer coachId,
        Integer roomId,Integer classId,Integer subjectId,LocalDate sessionDate,LocalTime startTime,LocalTime endTime,
        String status,Integer proposedBy,LocalDateTime createdAt,LocalDateTime respondBy,LocalDateTime respondedAt,
        Integer sessionId,SessionResponse session,LocalDateTime serverTime) {
    static PrivateSessionProposalResponse from(PrivateSessionProposal p,Clock clock) {
        LocalDateTime now=LocalDateTime.now(clock);
        String status="Pending".equals(p.getStatus())&&now.isAfter(p.getRespondBy())?"Expired":p.getStatus();
        return new PrivateSessionProposalResponse(p.getId(),p.getSubscription().getSubscriptionId(),p.getSubscription().getMember().getId(),
                p.getCoachId(),p.getRoomId(),p.getClassId(),p.getSubjectId(),p.getSessionDate(),p.getStartTime(),p.getEndTime(),status,
                p.getProposedBy().getId(),p.getCreatedAt(),p.getRespondBy(),p.getRespondedAt(),
                p.getSession()==null?null:p.getSession().getId(),p.getSession()==null?null:SessionResponse.from(p.getSession(),"Cancelled".equals(p.getSession().getStatus())?0:1),now);
    }
}
