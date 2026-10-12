package com.drmayday.authrbac.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "parent_role_id")
    private Integer parentRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id", insertable = false, updatable = false)
    private Role parentRole;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Role() {
    }

    public Role(Integer roleId, String name, String description, Integer parentRoleId) {
        this.roleId = roleId;
        this.name = name;
        this.description = description;
        this.parentRoleId = parentRoleId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getParentRoleId() {
        return parentRoleId;
    }

    public void setParentRoleId(Integer parentRoleId) {
        this.parentRoleId = parentRoleId;
    }

    public Role getParentRole() {
        return parentRole;
    }

    public void setParentRole(Role parentRole) {
        this.parentRole = parentRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer roleId;
        private String name;
        private String description;
        private Integer parentRoleId;

        public Builder roleId(Integer roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parentRoleId(Integer parentRoleId) {
            this.parentRoleId = parentRoleId;
            return this;
        }

        public Role build() {
            return new Role(roleId, name, description, parentRoleId);
        }
    }
}
