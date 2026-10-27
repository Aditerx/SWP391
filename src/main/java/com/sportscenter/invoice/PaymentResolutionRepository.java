package com.sportscenter.invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.*;
public interface PaymentResolutionRepository extends JpaRepository<PaymentResolution,Integer> {
    Optional<PaymentResolution> findByInvoiceInvoiceIdAndType(Integer id,String type);
    List<PaymentResolution> findByInvoiceInvoiceId(Integer id);
    @EntityGraph(attributePaths={"invoice","invoice.member"})
    List<PaymentResolution> findByInvoiceMemberCenterId(Integer centerId);
}
