package com.example.drmayday.entity;

import lombok.*;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class RolePermissionId implements Serializable {
    @Column(name = "ma_vai_tro", nullable = false)
    private Long roleId;

    @Column(name = "ma_quyen_han", nullable = false)
    private Long permissionId;
}
