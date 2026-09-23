package com.sportscenter.report;

import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final UserRepository userRepository;
    private final SportsClassRepository sportsClassRepository;
    private final MembershipPackageRepository membershipPackageRepository;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                userRepository.count(),
                userRepository.countMembers(),
                sportsClassRepository.count(),
                membershipPackageRepository.count());
    }
}
