package com.sportscenter.normal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RefundAttemptRepository extends JpaRepository<RefundAttempt,Integer> {
    List<RefundAttempt> findByRefundIdOrderByIdDesc(Integer id);
    Optional<RefundAttempt> findByClaimKey(String key);
    boolean existsByBankReferenceAndIdNot(String reference,Integer id);
}
