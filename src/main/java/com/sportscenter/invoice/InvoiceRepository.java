package com.sportscenter.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Invoice> {
    boolean existsBySubscriptionSubscriptionIdAndPaymentStatusAndReconciliationRequiredFalse(Integer subscriptionId, String status);
    @Query("select i.member.centerId from Invoice i where i.invoiceId = :id")
    java.util.Optional<Integer> findCenterIdById(@Param("id") Integer id);
    @Query("select i.member.centerId from Invoice i where i.invoiceCode = :code")
    java.util.Optional<Integer> findCenterIdByCode(@Param("code") String code);
    boolean existsByMemberIdAndSportsClassIdAndPaymentStatusAndInvoiceIdNot(Integer memberId, Integer classId, String status, Integer invoiceId);

    @Query(value = "SELECT nextval('invoice_code_seq')", nativeQuery = true)
    long nextInvoiceCodeValue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // Lock the root row in the SELECT itself. Fetch joins cause Hibernate follow-on locking,
    // which can leave a concurrent caller holding a stale Pending snapshot after waiting.
    @Query("SELECT i FROM Invoice i WHERE i.invoiceId = :id")
    java.util.Optional<Invoice> findByIdForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.invoiceCode = :code")
    java.util.Optional<Invoice> findByInvoiceCodeForUpdate(@Param("code") String code);

    List<Invoice> findByMemberIdOrderByInvoiceIdDesc(Integer memberId);

    List<Invoice> findByPaymentStatusAndExpiresAtBefore(String status, LocalDateTime now);

    boolean existsByMemberIdAndMembershipPackageIdAndPaymentStatus(Integer memberId, Integer packageId, String status);
    boolean existsBySubscriptionSubscriptionIdAndPaymentStatus(Integer subscriptionId, String status);
    List<Invoice> findBySubscriptionSubscriptionId(Integer subscriptionId);

    @Query("SELECT i FROM Invoice i " +
           "LEFT JOIN FETCH i.member m " +
           "LEFT JOIN FETCH i.membershipPackage p " +
           "LEFT JOIN FETCH i.receptionist r " +
           "ORDER BY i.invoiceId DESC")
    List<Invoice> findAllWithDetails();

    @Query("SELECT i FROM Invoice i " +
           "LEFT JOIN FETCH i.member m " +
           "LEFT JOIN FETCH i.membershipPackage p " +
           "LEFT JOIN FETCH i.receptionist r " +
           "WHERE i.member.id = :memberId " +
           "ORDER BY i.invoiceId DESC")
    List<Invoice> findByMemberId(@Param("memberId") Integer memberId);

    default List<Invoice> searchInvoices(
            @Param("memberId") Integer memberId,
            @Param("packageId") Integer packageId,
            @Param("receptionistId") Integer receptionistId,
            @Param("status") String status,
            @Param("method") String method,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    ) {
        return findAll((root, query, cb) -> {
            root.fetch("member", jakarta.persistence.criteria.JoinType.LEFT);
            root.fetch("membershipPackage", jakarta.persistence.criteria.JoinType.LEFT);
            root.fetch("sportsClass", jakarta.persistence.criteria.JoinType.LEFT);
            root.fetch("receptionist", jakarta.persistence.criteria.JoinType.LEFT);
            var member = root.join("member", jakarta.persistence.criteria.JoinType.LEFT);
            var pkg = root.join("membershipPackage", jakarta.persistence.criteria.JoinType.LEFT);
            var cls = root.join("sportsClass", jakarta.persistence.criteria.JoinType.LEFT);
            var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (memberId != null) conditions.add(cb.equal(member.get("id"), memberId));
            if (packageId != null) conditions.add(cb.equal(pkg.get("id"), packageId));
            if (receptionistId != null) conditions.add(cb.equal(root.get("receptionist").get("id"), receptionistId));
            if (status != null) conditions.add(cb.equal(cb.lower(root.get("paymentStatus")), status.toLowerCase(java.util.Locale.ROOT)));
            if (method != null) conditions.add(cb.equal(cb.lower(root.get("paymentMethod")), method.toLowerCase(java.util.Locale.ROOT)));
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(java.util.Locale.ROOT) + "%";
                conditions.add(cb.or(cb.like(cb.lower(root.get("invoiceCode")), pattern),
                        cb.like(cb.lower(member.get("fullName")), pattern),
                        cb.like(cb.lower(pkg.get("name")), pattern), cb.like(cb.lower(cls.get("name")), pattern)));
            }
            if (startDate != null) conditions.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), startDate));
            if (endDate != null) conditions.add(cb.lessThanOrEqualTo(root.get("paymentDate"), endDate));
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "invoiceId"));
    }

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i " +
           "WHERE i.paymentStatus = 'Paid' " +
           "AND (:startDate IS NULL OR i.paymentDate >= :startDate) " +
           "AND (:endDate IS NULL OR i.paymentDate <= :endDate)")
    BigDecimal sumRevenueBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(i) FROM Invoice i " +
           "WHERE i.paymentStatus = 'Paid' " +
           "AND (:startDate IS NULL OR i.paymentDate >= :startDate) " +
           "AND (:endDate IS NULL OR i.paymentDate <= :endDate)")
    long countPaidBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
