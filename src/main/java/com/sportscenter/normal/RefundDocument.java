package com.sportscenter.normal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="normal_refund_documents") @Getter @Setter
public class RefundDocument {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
    @Column(name="attempt_id",nullable=false) private Integer attemptId;
    @Column(name="uploaded_by",nullable=false) private Integer uploadedBy;
    @Column(name="uploaded_at",nullable=false) private LocalDateTime uploadedAt;
    @Column(name="content_type",nullable=false,length=30) private String contentType;
    @Column(nullable=false,length=64) private String sha256;
    @Column(nullable=false,columnDefinition="bytea") private byte[] content;
}
