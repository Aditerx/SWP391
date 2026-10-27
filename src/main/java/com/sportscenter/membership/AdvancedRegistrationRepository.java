package com.sportscenter.membership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
public interface AdvancedRegistrationRepository extends JpaRepository<AdvancedRegistration, Integer> {
    Optional<AdvancedRegistration> findByMemberIdAndRequestKey(Integer memberId, String key);
    List<AdvancedRegistration> findByMemberCenterId(Integer centerId);
}
