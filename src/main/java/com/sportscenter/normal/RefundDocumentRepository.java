package com.sportscenter.normal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RefundDocumentRepository extends JpaRepository<RefundDocument,Integer> {
    List<RefundDocument> findByAttemptIdOrderByIdAsc(Integer attempt);
    boolean existsByAttemptId(Integer attempt);
}
