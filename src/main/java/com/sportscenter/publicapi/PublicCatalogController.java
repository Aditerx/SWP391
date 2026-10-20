package com.sportscenter.publicapi;

import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.membership.PublicMembershipPackageResponse;
import com.sportscenter.subject.PublicSubjectResponse;
import com.sportscenter.subject.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicCatalogController {
    private final SubjectRepository subjectRepository;
    private final MembershipPackageRepository packageRepository;

    @GetMapping("/subjects")
    public List<PublicSubjectResponse> subjects() {
        return subjectRepository.findAll().stream()
                .filter(subject -> "Active".equalsIgnoreCase(subject.getStatus()))
                .map(PublicSubjectResponse::from).toList();
    }

    @GetMapping("/membership-packages")
    public List<PublicMembershipPackageResponse> packages(@RequestParam(defaultValue = "1") Integer centerId) {
        return packageRepository.findByCenterIdAndStatusIgnoreCase(centerId, "Active").stream()
                .map(PublicMembershipPackageResponse::from).toList();
    }
}
