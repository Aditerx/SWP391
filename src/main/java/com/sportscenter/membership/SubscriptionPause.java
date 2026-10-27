package com.sportscenter.membership;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** Historical closure evidence is immutable once the corresponding time has passed. */
@Entity
@Table(name = "subscription_pauses", uniqueConstraints = @UniqueConstraint(columnNames = {"subscription_id", "source_key"}))
@Getter @Setter
public class SubscriptionPause {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subscription_id", nullable = false)
    private MemberPackage subscription;
    @Column(name = "source_key", nullable = false, length = 120) private String sourceKey;
    @Column(name = "starts_at", nullable = false) private LocalDateTime startsAt;
    @Column(name = "ends_at", nullable = false) private LocalDateTime endsAt;
}
