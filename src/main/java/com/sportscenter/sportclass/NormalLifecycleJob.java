package com.sportscenter.sportclass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
@Component @RequiredArgsConstructor
public class NormalLifecycleJob {
    private final SportsClassRepository classes;
    private final NormalLifecycleService lifecycle;
    private final com.sportscenter.enrollment.EnrollmentService enrollments;
    private final com.sportscenter.normal.NormalChangeService changes;
    private final com.sportscenter.normal.NormalRefundService refunds;
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(NormalLifecycleJob.class);
    @Scheduled(fixedDelay=60_000L)
    public void reconcile() {
        for(Integer id:classes.findIdsAwaitingNormalOpening()) {
            try { lifecycle.reconcile(id); enrollments.promoteWaitlist(id,null); changes.remind(id); } catch(RuntimeException error) { log.error("Normal lifecycle failed for class {}",id,error); }
        }
        try { refunds.remind(); } catch(RuntimeException error) { log.error("Normal refund reminders failed",error); }
    }
}
