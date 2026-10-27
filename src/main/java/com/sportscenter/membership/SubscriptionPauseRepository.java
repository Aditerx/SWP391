package com.sportscenter.membership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SubscriptionPauseRepository extends JpaRepository<SubscriptionPause, Integer> {
    List<SubscriptionPause> findBySubscriptionSubscriptionId(Integer id);
}
