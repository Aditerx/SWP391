package com.sportscenter.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Integer> {
    long countByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(String email, String purpose, LocalDateTime since);

    Optional<EmailOtp> findFirstByEmailIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email, String purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from EmailOtp o where lower(o.email) = lower(:email) and o.purpose = :purpose "
            + "and o.used = false order by o.createdAt desc")
    java.util.List<EmailOtp> findUnusedForUpdate(@Param("email") String email, @Param("purpose") String purpose);
}
