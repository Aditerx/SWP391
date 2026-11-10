package com.sportscenter.normal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface NormalResponseRepository extends JpaRepository<NormalResponse,Integer> {
    List<NormalResponse> findByProposalId(Integer id);
    Optional<NormalResponse> findByProposalIdAndInvoiceId(Integer proposal,Integer invoice);
}
