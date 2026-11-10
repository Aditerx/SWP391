package com.sportscenter.normal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RefundReceiverRepository extends JpaRepository<RefundReceiver,Integer> {
    List<RefundReceiver> findByRefundIdOrderByIdAsc(Integer id);
}
