package com.sportscenter.membership;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPackageService {
    private final MembershipPackageRepository repository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<MembershipPackageResponse> findAll() {
        return repository.findAll().stream().map(MembershipPackageResponse::from).toList();
    }

    @Transactional
    public MembershipPackageResponse create(MembershipPackageRequest request) {
        MembershipPackage membershipPackage = new MembershipPackage();
        apply(membershipPackage, request);
        MembershipPackage saved = repository.save(membershipPackage);
        auditService.log(null, "CREATE_PACKAGE", "MEMBERSHIP_PACKAGE", saved.getId(), saved.getName());
        return MembershipPackageResponse.from(saved);
    }

    @Transactional
    public MembershipPackageResponse update(Integer id, MembershipPackageRequest request) {
        MembershipPackage membershipPackage = getEntity(id);
        apply(membershipPackage, request);
        return MembershipPackageResponse.from(repository.save(membershipPackage));
    }

    @Transactional
    public MembershipPackageResponse updateStatus(Integer id, String status) {
        MembershipPackage membershipPackage = getEntity(id);
        membershipPackage.setStatus(status);
        return MembershipPackageResponse.from(repository.save(membershipPackage));
    }

    private MembershipPackage getEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership package not found: " + id));
    }

    private void apply(MembershipPackage membershipPackage, MembershipPackageRequest request) {
        membershipPackage.setName(request.name());
        membershipPackage.setDurationDays(request.durationDays());
        membershipPackage.setPrice(request.price());
        membershipPackage.setBenefits(request.benefits());
        membershipPackage.setStatus(request.status() == null ? "ACTIVE" : request.status());
    }
}
