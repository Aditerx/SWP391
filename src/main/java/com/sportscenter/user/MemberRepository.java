package com.sportscenter.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.userId = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Integer id);
    @Query("SELECT m FROM Member m JOIN FETCH m.user u JOIN FETCH u.role r")
    List<Member> findAllWithUser();
}
