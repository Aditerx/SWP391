package com.sportscenter.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    @Query("SELECT m FROM Member m JOIN FETCH m.user u JOIN FETCH u.role r")
    List<Member> findAllWithUser();
}
