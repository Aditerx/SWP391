package com.sportscenter.holiday;

import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.*;
import com.sportscenter.membership.*;
import com.sportscenter.session.*;
import com.sportscenter.audit.AuditService;
import com.sportscenter.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Service @RequiredArgsConstructor
public class HolidayImpactService {
    private final HolidayRepository holidays;
    private final MemberPackageRepository subscriptions;
    private final SubscriptionPauseRepository pauses;
    private final EntitlementTimePolicy timePolicy;
    private final SessionRepository sessions;
    private final ScheduleWriteLock mutex;
    private final ActorPolicy actors;
    private final AuditService audit;
    private final Clock clock;
    public record Change(Integer holidayId,LocalDate holidayDate,String holidayName,String tag,Boolean active) { }
    public record DeadlineChange(Integer subscriptionId,LocalDateTime oldDeadline,LocalDateTime newDeadline) { }
    public record Preview(String previewToken,boolean blocked,List<Integer> affectedSessionIds,List<Integer> reconciliationSubscriptionIds,List<DeadlineChange> deadlineChanges) { }
    private record Proposal(Holiday original,boolean active,Preview preview) { }

    @Transactional(readOnly=true)
    public Preview preview(Change change,Authentication auth){User actor=actors.actor(auth);ActorPolicy.manager(actor);return proposal(change,actor).preview();}

    @Transactional
    public HolidayResponse confirm(Change change,String token,Authentication auth){
        User actor=actors.actor(auth);ActorPolicy.manager(actor);mutex.acquire(actor.getCenterId());
        Proposal p=proposal(change,actor);
        if(token==null||token.isBlank())throw new BusinessException("HOLIDAY_PREVIEW_REQUIRED: Preview the exact change first");
        if(!p.preview().previewToken().equals(token))throw new BusinessConflictException("HOLIDAY_PREVIEW_STALE: Preview changed; refresh before confirming");
        if(p.preview().blocked())throw new BusinessConflictException("HOLIDAY_IMPACT_UNRESOLVED: Resolve affected sessions or legacy entitlements separately first");
        Holiday h=p.original();
        if(h==null){h=new Holiday(change.holidayDate(),change.holidayName(),change.tag()==null?"CENTER_EVENT":change.tag(),p.active());}
        else{
            if(h.isActive()&&!p.active()){
                LocalDateTime preserved=preservedUntil(h);
                if(preserved!=null&&(h.getActualClosedUntil()==null||preserved.isAfter(h.getActualClosedUntil())))h.setActualClosedUntil(preserved);
            }
            h.setActive(p.active());
        }
        holidays.saveAndFlush(h);
        for(MemberPackage sub:subscriptions.findByMemberCenterId(actor.getCenterId())){
            if("Active".equals(sub.getStatus())&&sub.isAdvanced()&&sub.getActivatedAt()!=null)timePolicy.recalculate(sub);
        }
        audit.log(actor.getId(),"CONFIRM_HOLIDAY_CHANGE","HOLIDAY",h.getId(),"active="+h.isActive()+"; date="+h.getHolidayDate()+"; deadlineChanges="+p.preview().deadlineChanges().size());
        return HolidayResponse.from(h);
    }

    private Proposal proposal(Change change,User actor){
        if(change==null)throw new BusinessException("HOLIDAY_CHANGE_REQUIRED: Supply a change");
        Holiday original=change.holidayId()==null?null:holidays.findById(change.holidayId()).orElseThrow(()->new ResourceNotFoundException("Holiday not found"));
        LocalDate date=original==null?change.holidayDate():original.getHolidayDate();
        boolean active=change.active()==null?(original==null||!original.isActive()):change.active();
        if(date==null)throw new BusinessException("HOLIDAY_DATE_REQUIRED: Supply a date");
        if(original==null){
            if(change.holidayName()==null||change.holidayName().isBlank()||change.holidayName().length()>120)throw new BusinessException("HOLIDAY_NAME_REQUIRED: Supply a holiday name");
            if(holidays.findByHolidayDate(date).isPresent())throw new BusinessConflictException("HOLIDAY_DUPLICATE: Date already exists");
            if(active&&!date.isAfter(LocalDate.now(clock)))throw new BusinessException("HOLIDAY_HISTORY_IMMUTABLE: Use an incident record for an unrecorded current/past closure");
        }else if(change.holidayDate()!=null&&!date.equals(change.holidayDate()))throw new BusinessException("HOLIDAY_DATE_IMMUTABLE: A toggle cannot change the date");
        if(original!=null&&!original.isActive()&&active&&!date.isAfter(LocalDate.now(clock)))throw new BusinessException("HOLIDAY_HISTORY_IMMUTABLE: Past closure evidence cannot be created by a toggle");
        if(change.tag()!=null&&!Set.of("NATIONAL_FIXED","LUNAR_FESTIVAL","CENTER_EVENT").contains(change.tag()))throw new BusinessException("HOLIDAY_TAG_INVALID: Unsupported tag");
        var allSubs=subscriptions.findAllWithDetails();
        var future=sessions.findFiltered(null,null,null,LocalDate.now(clock),null,"Scheduled");
        // Holidays are global in the existing schema. Do not silently affect another center.
        if(allSubs.stream().anyMatch(s->"Active".equals(s.getStatus())&&!Objects.equals(s.getMember().getCenterId(),actor.getCenterId()))
                ||future.stream().anyMatch(s->!Objects.equals(s.getSportsClass().getCenterId(),actor.getCenterId())))
            throw new BusinessConflictException("HOLIDAY_SCOPE_REVIEW_REQUIRED: Global holiday affects another center");
        List<EntitlementTimePolicy.Interval> closures=new ArrayList<>();
        var allHolidays=holidays.findAllByOrderByHolidayDateAsc();
        for(Holiday h:allHolidays){
            boolean selected=original!=null&&Objects.equals(h.getId(),original.getId());
            boolean enabled=selected?active:h.isActive();
            if(enabled)closures.add(new EntitlementTimePolicy.Interval(h.getHolidayDate().atStartOfDay(),h.getHolidayDate().plusDays(1).atStartOfDay()));
            LocalDateTime preserved=h.getActualClosedUntil();
            if(selected&&h.isActive()&&!active){var actual=preservedUntil(h);if(actual!=null&&(preserved==null||actual.isAfter(preserved)))preserved=actual;}
            if(preserved!=null)closures.add(new EntitlementTimePolicy.Interval(h.getHolidayDate().atStartOfDay(),preserved));
        }
        if(original==null&&active)closures.add(new EntitlementTimePolicy.Interval(date.atStartOfDay(),date.plusDays(1).atStartOfDay()));
        Set<Integer> affected=new TreeSet<>(),reconciliation=new TreeSet<>();
        boolean actualChange=original==null||original.isActive()!=active;
        if(actualChange&&active)future.stream().filter(s->s.getSessionDate().equals(date)&&!s.isCenterInvalidated()).forEach(s->affected.add(s.getId()));
        List<DeadlineChange> deadlines=new ArrayList<>();
        if(actualChange)for(MemberPackage sub:allSubs){
            if(!"Active".equals(sub.getStatus())||!sub.isAdvanced())continue;
            if(sub.getActivatedAt()==null||sub.getExpiresAt()==null){reconciliation.add(sub.getSubscriptionId());continue;}
            List<EntitlementTimePolicy.Interval> all=new ArrayList<>(closures);
            pauses.findBySubscriptionSubscriptionId(sub.getSubscriptionId()).forEach(p->all.add(new EntitlementTimePolicy.Interval(p.getStartsAt(),p.getEndsAt())));
            LocalDateTime proposed=EntitlementTimePolicy.deadline(sub.getActivatedAt(),Duration.ofSeconds(sub.getApprovedExtraSeconds()).plusNanos(sub.getApprovedExtraNanos()),all);
            if(!proposed.equals(sub.getExpiresAt()))deadlines.add(new DeadlineChange(sub.getSubscriptionId(),sub.getExpiresAt(),proposed));
            future.stream().filter(s->s.getMemberPackage()!=null&&Objects.equals(s.getMemberPackage().getSubscriptionId(),sub.getSubscriptionId()))
                    .filter(s->s.getSessionDate().atTime(s.getEndTime()).isAfter(proposed)).forEach(s->affected.add(s.getId()));
        }
        String state=change+";active="+active+";date="+LocalDate.now(clock)+";holidays="+allHolidays.stream().map(h->h.getId()+":"+h.isActive()+":"+h.getHolidayDate()+":"+h.getActualClosedUntil()).toList()
                +";subscriptions="+allSubs.stream().sorted(Comparator.comparing(MemberPackage::getSubscriptionId)).map(s->s.getSubscriptionId()+":"+s.getStatus()+":"+s.getExpiresAt()+":"+s.getRemainingCredit()+":"+s.getApprovedExtraSeconds()+":"+s.getApprovedExtraNanos()).toList()
                +";sessions="+future.stream().sorted(Comparator.comparing(Session::getId)).map(s->s.getId()+":"+s.getSessionDate()+":"+s.getStartTime()+":"+s.getEndTime()+":"+s.getStatus()+":"+s.getRoom().getId()+":"+s.isCenterInvalidated()).toList();
        try{
            String token=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(state.getBytes(StandardCharsets.UTF_8)));
            return new Proposal(original,active,new Preview(token,!affected.isEmpty()||!reconciliation.isEmpty(),List.copyOf(affected),List.copyOf(reconciliation),deadlines));
        }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private LocalDateTime preservedUntil(Holiday h){
        LocalDateTime now=LocalDateTime.now(clock),start=h.getHolidayDate().atStartOfDay(),end=start.plusDays(1);
        if(!now.isAfter(start))return null;return now.isBefore(end)?now:end;
    }
}
