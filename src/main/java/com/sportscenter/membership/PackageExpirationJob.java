package com.sportscenter.membership;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PackageExpirationJob {
    private final MemberPackageService memberPackageService;

    @Scheduled(cron = "${scms.package-expiration.cron:0 0 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void scanExpiredPackages() {
        try {
            int count = memberPackageService.expireOutdatedPackages();
            if (count > 0) {
                log.info("Midnight cron job expired {} outdated member packages and cleared credits", count);
            }
        } catch (RuntimeException exception) {
            log.error("PackageExpirationJob: Failed to expire outdated member packages", exception);
        }
    }
}
