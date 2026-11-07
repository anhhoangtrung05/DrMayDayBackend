package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "nhanvien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee implements Persistable<Long> {
    @Id
    @Column(name = "ma_nhan_vien")
    private Long employeeId;

    @Column(name = "ma_chi_nhanh", nullable = false)
    private Long branchId;

    @Transient
    @Builder.Default
    private boolean newEntity = true;

    @Override
    public Long getId() {
        return employeeId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markExisting() {
        newEntity = false;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nhan_vien", referencedColumnName = "ma_nguoi", insertable = false, updatable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chi_nhanh", referencedColumnName = "ma_chi_nhanh", insertable = false, updatable = false)
    private Branch branch;
}
