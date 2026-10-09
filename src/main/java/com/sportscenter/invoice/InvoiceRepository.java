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
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    @Query(value = "SELECT nextval('invoice_code_seq')", nativeQuery = true)
    long nextInvoiceCodeValue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.member LEFT JOIN FETCH i.membershipPackage " +
           "LEFT JOIN FETCH i.sportsClass LEFT JOIN FETCH i.subscription LEFT JOIN FETCH i.receptionist WHERE i.invoiceId = :id")
    java.util.Optional<Invoice> findByIdForUpdate(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.member LEFT JOIN FETCH i.membershipPackage " +
           "LEFT JOIN FETCH i.sportsClass LEFT JOIN FETCH i.subscription WHERE i.invoiceCode = :code")
    java.util.Optional<Invoice> findByInvoiceCodeForUpdate(@Param("code") String code);

    List<Invoice> findByMemberIdOrderByInvoiceIdDesc(Integer memberId);

    List<Invoice> findByPaymentStatusAndExpiresAtBefore(String status, LocalDateTime now);

    boolean existsByMemberIdAndMembershipPackageIdAndPaymentStatus(Integer memberId, Integer packageId, String status);

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

    @Query("SELECT i FROM Invoice i " +
           "LEFT JOIN FETCH i.member m " +
           "LEFT JOIN FETCH i.membershipPackage p " +
           "LEFT JOIN FETCH i.receptionist r " +
           "WHERE (:memberId IS NULL OR i.member.id = :memberId) " +
           "AND (:packageId IS NULL OR (i.membershipPackage IS NOT NULL AND i.membershipPackage.id = :packageId)) " +
           "AND (:receptionistId IS NULL OR (i.receptionist IS NOT NULL AND i.receptionist.id = :receptionistId)) " +
           "AND (:status IS NULL OR LOWER(i.paymentStatus) = LOWER(:status)) " +
           "AND (:method IS NULL OR LOWER(i.paymentMethod) = LOWER(:method)) " +
           "AND (:keyword IS NULL OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(i.member.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR (i.membershipPackage IS NOT NULL AND LOWER(i.membershipPackage.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "OR (i.sportsClass IS NOT NULL AND LOWER(i.sportsClass.name) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "AND (:startDate IS NULL OR i.paymentDate >= :startDate) " +
           "AND (:endDate IS NULL OR i.paymentDate <= :endDate) " +
           "ORDER BY i.invoiceId DESC")
    List<Invoice> searchInvoices(
            @Param("memberId") Integer memberId,
            @Param("packageId") Integer packageId,
            @Param("receptionistId") Integer receptionistId,
            @Param("status") String status,
            @Param("method") String method,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

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
