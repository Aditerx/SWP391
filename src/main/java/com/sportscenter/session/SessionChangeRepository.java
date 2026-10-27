package com.sportscenter.session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SessionChangeRepository extends JpaRepository<SessionChange,Integer> {
    Optional<SessionChange> findBySessionIdAndRequestKey(Integer id,String key);
}
