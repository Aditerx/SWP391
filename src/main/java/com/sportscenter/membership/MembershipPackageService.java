package com.sportscenter.membership;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPackageService {
    private final MembershipPackageRepository repository;

    @Transactional(readOnly = true)
    public List<MembershipPackageResponse> findAll() {
        return repository.findAll().stream().map(MembershipPackageResponse::from).toList();
    }

    @Transactional
    public MembershipPackageResponse create(MembershipPackageRequest request) {
        throw new com.sportscenter.common.exception.BusinessException("LEGACY_MEMBERSHIP_RETIRED: Shared membership catalog cannot be created or changed");
    }

    @Transactional
    public MembershipPackageResponse update(Integer id, MembershipPackageRequest request) {
        throw new com.sportscenter.common.exception.BusinessException("LEGACY_MEMBERSHIP_RETIRED: Shared membership catalog cannot be created or changed");
    }

    @Transactional
    public MembershipPackageResponse updateStatus(Integer id, String status) {
        throw new com.sportscenter.common.exception.BusinessException("LEGACY_MEMBERSHIP_RETIRED: Shared membership catalog cannot be created or changed");
    }

}
