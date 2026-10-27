package com.sportscenter.session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CenterIncidentRepository extends JpaRepository<CenterIncident,Integer> {
    Optional<CenterIncident> findBySessionIdAndRequestKey(Integer sessionId,String key);
    List<CenterIncident> findBySessionSportsClassCenterId(Integer centerId);
}
