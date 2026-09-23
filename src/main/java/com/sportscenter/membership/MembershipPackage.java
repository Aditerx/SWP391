package com.sportscenter.membership;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "membership_packages")
@Getter
@Setter
@NoArgsConstructor
public class MembershipPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "package_id")
    private Integer id;

    @Column(name = "package_name")
    private String name;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "benefits")
    private String benefits;

    @Column(name = "status")
    private String status;
}
