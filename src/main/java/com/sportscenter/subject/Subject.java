package com.sportscenter.subject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    private Integer id;

    @Column(name = "subject_name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "normal_price", precision = 12, scale = 2)
    private BigDecimal normalPrice = BigDecimal.ZERO;

    @Column(name = "advanced_price", precision = 12, scale = 2)
    private BigDecimal advancedPrice = BigDecimal.ZERO;

    @Column(name = "status", nullable = false)
    private String status = "Active";

    @Column(name = "slug", unique = true)
    private String slug;

    public boolean isOutdoor() {
        if (slug != null) {
            String s = slug.toLowerCase();
            if (s.contains("tennis") || s.contains("boi-loi") || s.contains("ban-cung") || s.contains("bong-da")) {
                return true;
            }
        }
        if (name != null) {
            String n = name.toLowerCase();
            if (n.contains("tennis") || n.contains("quần vợt") || n.contains("bơi lội") || n.contains("bắn cung") || n.contains("bóng đá")) {
                return true;
            }
        }
        return false;
    }
}
