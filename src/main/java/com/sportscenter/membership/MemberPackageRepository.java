package com.sportscenter.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberPackageRepository extends JpaRepository<MemberPackage, Integer> {
    @Query("SELECT mp FROM MemberPackage mp " +
           "JOIN FETCH mp.member u " +
           "JOIN FETCH mp.membershipPackage p " +
           "WHERE u.id = :memberId")
    List<MemberPackage> findByMemberId(@Param("memberId") Integer memberId);

    @Query("SELECT mp FROM MemberPackage mp " +
           "JOIN FETCH mp.member u " +
           "JOIN FETCH mp.membershipPackage p")
    List<MemberPackage> findAllWithDetails();
}
