package com.example.drmayday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@IdClass(RolePermissionId.class)
@Table(name = "vaitroquyenhan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {
    @Id
    @Column(name = "ma_vai_tro", nullable = false) private Long roleId;
    @Id
    @Column(name = "ma_quyen_han", nullable = false) private Long permissionId;
    @Column(name = "thoi_diem_gan", nullable = false) private OffsetDateTime assignedAt;
    @Column(name = "ma_nguoi_gan") private Long assignedByPersonId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_vai_tro", referencedColumnName = "ma_vai_tro", insertable = false, updatable = false)
    private Role role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_quyen_han", referencedColumnName = "ma_quyen_han", insertable = false, updatable = false)
    private Permission permission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nguoi_gan", referencedColumnName = "ma_nguoi", insertable = false, updatable = false)
    private Person person;
}
