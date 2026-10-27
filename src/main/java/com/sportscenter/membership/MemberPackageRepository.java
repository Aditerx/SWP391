package com.sportscenter.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberPackageRepository extends JpaRepository<MemberPackage, Integer> {
    @Query("select mp.member.centerId from MemberPackage mp where mp.subscriptionId = :id")
    Optional<Integer> findCenterIdById(@Param("id") Integer id);
    @Query("select mp.subscriptionId from MemberPackage mp where mp.status = 'Active' and mp.endDate < :today")
    List<Integer> findExpiredActivePackageIds(@Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mp FROM MemberPackage mp WHERE mp.subscriptionId = :id")
    Optional<MemberPackage> findByIdForUpdate(@Param("id") Integer id);

    @Query("SELECT mp FROM MemberPackage mp " +
           "JOIN FETCH mp.member u " +
           "JOIN FETCH mp.membershipPackage p " +
           "WHERE u.id = :memberId")
    List<MemberPackage> findByMemberId(@Param("memberId") Integer memberId);

    @Query("SELECT mp FROM MemberPackage mp " +
           "JOIN FETCH mp.member u " +
           "JOIN FETCH mp.membershipPackage p " +
           "WHERE u.id = :memberId AND mp.status = :status")
    List<MemberPackage> findByMemberIdAndStatus(@Param("memberId") Integer memberId, @Param("status") String status);

    @Query("SELECT mp FROM MemberPackage mp " +
           "JOIN FETCH mp.member u " +
           "JOIN FETCH mp.membershipPackage p")
    List<MemberPackage> findAllWithDetails();

    @Query("SELECT mp FROM MemberPackage mp WHERE mp.status = 'Active' AND mp.endDate < :today")
    List<MemberPackage> findExpiredActivePackages(@Param("today") LocalDate today);

    @Query("SELECT mp FROM MemberPackage mp JOIN FETCH mp.member m JOIN FETCH mp.membershipPackage p " +
            "WHERE mp.status = 'Active' AND p.packageType = 'Advanced' AND mp.endDate BETWEEN :fromDate AND :toDate")
    List<MemberPackage> findActiveAdvancedExpiringBetween(@Param("fromDate") LocalDate fromDate,
                                                          @Param("toDate") LocalDate toDate);
}
