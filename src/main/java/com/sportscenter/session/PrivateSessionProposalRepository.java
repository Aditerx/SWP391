package com.sportscenter.session;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface PrivateSessionProposalRepository extends JpaRepository<PrivateSessionProposal,Integer> {
    Optional<PrivateSessionProposal> findBySubscriptionSubscriptionIdAndRequestKey(Integer subscriptionId,String requestKey);
    @Query("select p.subscription.member.centerId from PrivateSessionProposal p where p.id=:id")
    Optional<Integer> findCenterId(@Param("id") Integer id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PrivateSessionProposal p where p.id=:id")
    Optional<PrivateSessionProposal> lockById(@Param("id") Integer id);
    List<PrivateSessionProposal> findBySubscriptionMemberCenterIdOrderByIdDesc(Integer centerId);
}
