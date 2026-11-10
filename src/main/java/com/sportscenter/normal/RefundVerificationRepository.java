package com.sportscenter.normal;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RefundVerificationRepository extends JpaRepository<RefundVerification,Integer> {
    java.util.List<RefundVerification> findByAttemptIdOrderByIdAsc(Integer attempt);
}
