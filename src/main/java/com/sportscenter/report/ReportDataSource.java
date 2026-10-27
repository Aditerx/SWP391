package com.sportscenter.report;
import com.sportscenter.user.*;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.membership.*;
import com.sportscenter.enrollment.ClassEnrollment;
import com.sportscenter.invoice.*;
import com.sportscenter.session.Session;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository @RequiredArgsConstructor
public class ReportDataSource {
    private final EntityManager em;
    private final InvoiceRepository invoices;
    private final PaymentResolutionRepository resolutions;
    public record Data(List<User> users,List<Member> members,List<SportsClass> classes,List<MembershipPackage> packages,
            List<MemberPackage> subscriptions,List<ClassEnrollment> enrollments,List<Session> sessions,List<Invoice> invoices,List<PaymentResolution> resolutions) { }
    public Data load(Integer center){
        return new Data(
                em.createQuery("select u from User u where u.centerId=:c",User.class).setParameter("c",center).getResultList(),
                em.createQuery("select m from Member m join fetch m.user u where u.centerId=:c",Member.class).setParameter("c",center).getResultList(),
                em.createQuery("select c from SportsClass c left join fetch c.subject where c.centerId=:c",SportsClass.class).setParameter("c",center).getResultList(),
                em.createQuery("select p from MembershipPackage p where p.centerId=:c",MembershipPackage.class).setParameter("c",center).getResultList(),
                em.createQuery("select p from MemberPackage p join fetch p.member u left join fetch p.membershipPackage left join fetch p.subject left join fetch p.primaryCoach where u.centerId=:c",MemberPackage.class).setParameter("c",center).getResultList(),
                em.createQuery("select e from ClassEnrollment e join fetch e.member u join fetch e.sportsClass where u.centerId=:c",ClassEnrollment.class).setParameter("c",center).getResultList(),
                em.createQuery("select s from Session s join fetch s.sportsClass c left join fetch s.memberPackage left join fetch s.bookedMember where c.centerId=:c",Session.class).setParameter("c",center).getResultList(),
                invoices.findByMemberCenterId(center),resolutions.findByInvoiceMemberCenterId(center));
    }
}
