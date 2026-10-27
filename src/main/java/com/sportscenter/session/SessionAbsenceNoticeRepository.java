package com.sportscenter.session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SessionAbsenceNoticeRepository extends JpaRepository<SessionAbsenceNotice,Integer> {
    Optional<SessionAbsenceNotice> findFirstBySessionIdAndActiveTrue(Integer id);
    List<SessionAbsenceNotice> findBySessionIdOrderByIdAsc(Integer id);
}
