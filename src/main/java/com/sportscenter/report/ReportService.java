package com.sportscenter.report;

import com.sportscenter.common.ActorPolicy;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.invoice.*;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.session.Session;
import com.sportscenter.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ReportService {
    private final Clock clock;
    private final ActorPolicy actors;
    private final ReportDataSource dataSource;
    private User manager(Authentication auth){
        User user=actors.actor(auth);ActorPolicy.manager(user);
        if(auth.getAuthorities().stream().noneMatch(a->"VIEW_REPORTS".equals(a.getAuthority())))throw new AccessDeniedException("VIEW_REPORTS required");
        return user;
    }
    @Transactional(readOnly=true) public DashboardResponse dashboard(){return dashboard(SecurityContextHolder.getContext().getAuthentication());}
    @Transactional(readOnly=true) public DashboardResponse dashboard(Authentication auth){
        var data=dataSource.load(manager(auth).getCenterId());var money=money(data,null,null);
        long active=data.subscriptions().stream().filter(s->hasAdvancedRight(s,data)).count()+data.enrollments().stream().filter(e->hasNormalRight(e,data)).count();
        return new DashboardResponse(data.users().size(),data.members().size(),data.classes().size(),data.packages().size(),
                sum(paid(data,null,null)),paid(data,null,null).size(),active,
                data.sessions().stream().filter(s->LocalDate.now(clock).equals(s.getSessionDate())&&!s.isCenterInvalidated()&&!"Cancelled".equals(s.getStatus())).count(),
                money.received,money.refunded,money.received.subtract(money.refunded));
    }
    @Transactional(readOnly=true) public RevenueReportResponse revenueReport(LocalDate from,LocalDate to,String group){return revenueReport(from,to,group,SecurityContextHolder.getContext().getAuthentication());}
    @Transactional(readOnly=true) public RevenueReportResponse revenueReport(LocalDate from,LocalDate to,String group,Authentication auth){
        if(from!=null&&to!=null&&to.isBefore(from))throw new BusinessException("INVALID_REPORT_RANGE: End date precedes start date");
        if(!Set.of("day","month").contains(group))throw new BusinessException("INVALID_REPORT_GROUP: Use day or month");
        var data=dataSource.load(manager(auth).getCenterId());var paid=paid(data,from,to);var money=money(data,from,to);BigDecimal gross=sum(paid);
        List<RevenueReportResponse.RevenueByPackageItem> packages=new ArrayList<>();
        paid.stream().collect(Collectors.groupingBy(this::packageName,TreeMap::new,Collectors.toList())).forEach((name,list)->packages.add(new RevenueReportResponse.RevenueByPackageItem(list.get(0).getMembershipPackage()==null?null:list.get(0).getMembershipPackage().getId(),name,list.size(),sum(list))));
        List<RevenueReportResponse.RevenueByMethodItem> methods=new ArrayList<>();
        paid.stream().collect(Collectors.groupingBy(i->i.getPaymentMethod()==null?"Unknown":i.getPaymentMethod(),TreeMap::new,Collectors.toList())).forEach((name,list)->methods.add(new RevenueReportResponse.RevenueByMethodItem(name,list.size(),sum(list))));
        var formatter=DateTimeFormatter.ofPattern("day".equals(group)?"yyyy-MM-dd":"yyyy-MM");
        List<RevenueReportResponse.RevenueTimelineItem> timeline=new ArrayList<>();
        paid.stream().filter(i->i.getPaymentDate()!=null).collect(Collectors.groupingBy(i->i.getPaymentDate().format(formatter),TreeMap::new,Collectors.toList()))
                .forEach((period,list)->timeline.add(new RevenueReportResponse.RevenueTimelineItem(period,sum(list),list.size())));
        packages.sort(Comparator.comparing(RevenueReportResponse.RevenueByPackageItem::totalRevenue).reversed());methods.sort(Comparator.comparing(RevenueReportResponse.RevenueByMethodItem::totalRevenue).reversed());
        return new RevenueReportResponse(gross,paid.size(),paid.isEmpty()?BigDecimal.ZERO:gross.divide(BigDecimal.valueOf(paid.size()),0,RoundingMode.HALF_UP),packages,methods,timeline,money.received,money.refunded,money.received.subtract(money.refunded),money.corrections);
    }
    @Transactional(readOnly=true) public MemberReportResponse memberReport(){return memberReport(SecurityContextHolder.getContext().getAuthentication());}
    @Transactional(readOnly=true) public MemberReportResponse memberReport(Authentication auth){
        var data=dataSource.load(manager(auth).getCenterId());Set<Integer> ever=new HashSet<>(),active=new HashSet<>(),unknown=new HashSet<>();
        long expired=0,completed=0,reconciliation=0;
        Map<String,Set<Integer>> distribution=new TreeMap<>();
        LocalDateTime now=LocalDateTime.now(clock);
        for(MemberPackage s:data.subscriptions()){
            if(!s.isAdvanced()||!paidEntitlement(s,data))continue;
            if(s.getSubject()==null||s.getPrimaryCoach()==null||s.getActivatedAt()==null||s.getExpiresAt()==null){unknown.add(s.getMember().getId());reconciliation++;continue;}
            ever.add(s.getMember().getId());
            if(hasAdvancedRight(s,data)){active.add(s.getMember().getId());distribution.computeIfAbsent(packageName(s),k->new HashSet<>()).add(s.getMember().getId());}
            if(!List.of("Cancelled","Completed").contains(s.getStatus())&&!s.getExpiresAt().isAfter(now))expired++;
            if("Completed".equals(s.getStatus()))completed++;
        }
        for(ClassEnrollment e:data.enrollments()){
            if(!normalPaid(e,data)||!List.of("Registered","Completed").contains(e.getStatus()))continue;
            var classSessions=data.sessions().stream().filter(s->Objects.equals(s.getSportsClass().getId(),e.getSportsClass().getId())).toList();
            long validCount=classSessions.stream().filter(s->!s.isCenterInvalidated()&&!"Cancelled".equals(s.getStatus())).count();
            if(validCount>12||validCount<12&&classSessions.stream().noneMatch(Session::isCenterInvalidated)){unknown.add(e.getMember().getId());reconciliation++;continue;}
            ever.add(e.getMember().getId());
            if(hasNormalRight(e,data)){active.add(e.getMember().getId());distribution.computeIfAbsent(e.getSportsClass().getName(),k->new HashSet<>()).add(e.getMember().getId());}
            if("Completed".equals(e.getStatus()))completed++;
        }
        Set<Integer> ids=data.members().stream().map(m->m.getUserId()).collect(Collectors.toSet());
        long previously=ids.stream().filter(ever::contains).filter(id->!active.contains(id)).count();
        long never=ids.stream().filter(id->!ever.contains(id)&&!unknown.contains(id)).count();
        long reconcilePeople=ids.stream().filter(unknown::contains).filter(id->!ever.contains(id)).count();
        var packageItems=distribution.entrySet().stream().map(e->new MemberReportResponse.PackageDistributionItem(e.getKey(),e.getValue().size())).toList();
        var month=DateTimeFormatter.ofPattern("yyyy-MM");Map<String,Long> growth=data.members().stream().filter(m->m.getJoinDate()!=null).collect(Collectors.groupingBy(m->m.getJoinDate().format(month),TreeMap::new,Collectors.counting()));
        return new MemberReportResponse(ids.size(),ids.stream().filter(active::contains).count(),previously,packageItems,
                growth.entrySet().stream().map(e->new MemberReportResponse.MonthlyMemberGrowthItem(e.getKey(),e.getValue())).toList(),never,previously,expired,completed,reconciliation,reconcilePeople);
    }
    private boolean paidEntitlement(MemberPackage sub,ReportDataSource.Data d){return d.invoices().stream().anyMatch(i->i.getSubscription()!=null&&Objects.equals(i.getSubscription().getSubscriptionId(),sub.getSubscriptionId())&&"Paid".equals(i.getPaymentStatus())&&!i.isReconciliationRequired()&&!i.isCashConfirmationReversed());}
    private boolean hasAdvancedRight(MemberPackage sub,ReportDataSource.Data data){
        if(!sub.isAdvanced()||!"Active".equals(sub.getStatus())||!paidEntitlement(sub,data)||sub.getActivatedAt()==null||sub.getExpiresAt()==null||sub.getSubject()==null||sub.getPrimaryCoach()==null||!sub.getExpiresAt().isAfter(LocalDateTime.now(clock)))return false;
        return sub.getRemainingCredit()!=null&&sub.getRemainingCredit()>0||data.sessions().stream().anyMatch(s->s.getMemberPackage()!=null&&Objects.equals(s.getMemberPackage().getSubscriptionId(),sub.getSubscriptionId())&&validFuture(s)&&!s.getSessionDate().atTime(s.getEndTime()).isAfter(sub.getExpiresAt()));
    }
    private boolean normalPaid(ClassEnrollment e,ReportDataSource.Data data){return e.getSportsClass().isNormal()&&data.invoices().stream().anyMatch(i->i.getSportsClass()!=null&&Objects.equals(i.getMember().getId(),e.getMember().getId())&&Objects.equals(i.getSportsClass().getId(),e.getSportsClass().getId())&&"Paid".equals(i.getPaymentStatus())&&!i.isReconciliationRequired()&&!i.isCashConfirmationReversed());}
    private boolean hasNormalRight(ClassEnrollment e,ReportDataSource.Data data){
        if(!"Registered".equals(e.getStatus())||!normalPaid(e,data))return false;
        var valid=data.sessions().stream().filter(s->Objects.equals(s.getSportsClass().getId(),e.getSportsClass().getId())&&!s.isCenterInvalidated()&&!"Cancelled".equals(s.getStatus())).toList();
        return valid.size()<12&&data.sessions().stream().anyMatch(s->Objects.equals(s.getSportsClass().getId(),e.getSportsClass().getId())&&s.isCenterInvalidated())||valid.size()==12&&valid.stream().anyMatch(this::validFuture);
    }
    private boolean validFuture(Session s){return "Scheduled".equals(s.getStatus())&&!s.isAttendanceFinalized()&&!s.isCenterInvalidated()&&s.getSessionDate()!=null&&s.getEndTime()!=null&&s.getSessionDate().atTime(s.getEndTime()).isAfter(LocalDateTime.now(clock));}
    private record Money(BigDecimal received,BigDecimal refunded,BigDecimal corrections) { }
    private Money money(ReportDataSource.Data data,LocalDate from,LocalDate to){
        BigDecimal received=paid(data,from,to).stream().filter(i->!i.isCashConfirmationReversed()&&i.getPaymentDate()!=null).map(Invoice::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal refunded=data.resolutions().stream().filter(r->"Completed".equals(r.getStatus())&&"Refund".equals(r.getType())&&r.getPerformedAt()!=null&&inRange(r.getPerformedAt(),from,to)).map(PaymentResolution::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal corrections=data.resolutions().stream().filter(r->"Completed".equals(r.getStatus())&&"CashCorrection".equals(r.getType())&&inRange(r.getCompletedAt(),from,to)).map(PaymentResolution::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new Money(received,refunded,corrections);
    }
    private List<Invoice> paid(ReportDataSource.Data data,LocalDate from,LocalDate to){return data.invoices().stream().filter(i->"Paid".equalsIgnoreCase(i.getPaymentStatus())).filter(i->inRange(i.getPaymentDate(),from,to)).toList();}
    private boolean inRange(LocalDateTime date,LocalDate from,LocalDate to){if(date==null)return from==null&&to==null;return(from==null||!date.isBefore(from.atStartOfDay()))&&(to==null||date.isBefore(to.plusDays(1).atStartOfDay()));}
    private BigDecimal sum(List<Invoice> invoices){return invoices.stream().map(Invoice::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private String packageName(Invoice i){return i.getMembershipPackage()!=null?i.getMembershipPackage().getName():i.getSubscription()!=null?packageName(i.getSubscription()):i.getSportsClass()!=null?i.getSportsClass().getName():"Other services";}
    private String packageName(MemberPackage s){return s.getSubject()!=null?s.getSubject().getName()+" Advanced":s.getMembershipPackage()!=null?s.getMembershipPackage().getName():"Reconciliation required";}
}
