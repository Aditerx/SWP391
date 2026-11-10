package com.sportscenter.normal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NormalRefundRepository extends JpaRepository<NormalRefund,Integer> {
    List<NormalRefund> findByResolutionInvoiceMemberCenterIdOrderByIdDesc(Integer center);
    List<NormalRefund> findByResolutionInvoiceMemberIdOrderByIdDesc(Integer member);
}
