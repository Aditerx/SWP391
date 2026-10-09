package com.sportscenter.membership;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPackageRepository extends JpaRepository<MembershipPackage, Integer> {
    java.util.List<MembershipPackage> findByCenterIdAndStatusIgnoreCase(Integer centerId, String status);
}
