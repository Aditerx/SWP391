package com.sportscenter.sportclass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SportsClassRepository extends JpaRepository<SportsClass, Integer> {
    @Query("select c.id from SportsClass c where c.classType='Normal' and c.status='Open' and c.pricingDraft=false")
    java.util.List<Integer> findIdsAwaitingNormalOpening();
    boolean existsBySubjectId(Integer subjectId);
    @Query("select c.centerId from SportsClass c where c.id = :id")
    Optional<Integer> findCenterIdById(@Param("id") Integer id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM SportsClass c WHERE c.id = :id")
    Optional<SportsClass> findByIdForUpdate(@Param("id") Integer id);

    Optional<SportsClass> findFirstBySubjectIdAndClassType(Integer subjectId, String classType);

    @Query("SELECT c FROM SportsClass c JOIN FETCH c.subject s WHERE c.status = 'Open' " +
            "AND c.pricingDraft = false AND c.classType = 'Normal' ORDER BY c.startDate, c.id")
    List<SportsClass> findPublicOpenClasses(Pageable pageable);

    @Query("SELECT c FROM SportsClass c JOIN FETCH c.subject s WHERE c.status = 'Open' " +
            "AND c.pricingDraft = false AND c.classType = 'Normal' " +
            "AND LOWER(s.name) = :subjectName ORDER BY c.startDate, c.id")
    List<SportsClass> findPublicOpenClassesBySubjectName(@Param("subjectName") String subjectName, Pageable pageable);
}
