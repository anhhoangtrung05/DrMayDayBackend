package com.drmayday.authrbac.entity;

import com.drmayday.facility.entity.Branch;
import com.drmayday.facility.entity.Department;
import com.drmayday.facility.entity.Room;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "staffs")
public class Staff {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

    @Column(name = "department_id")
    private Integer departmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", insertable = false, updatable = false)
    private Department department;

    @Column(name = "branch_id")
    private Integer branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    private Branch branch;

    @Column(name = "room_id")
    private Integer roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private Room room;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Staff() {
    }

    public Staff(Integer userId, User user, Integer roleId, Role role, Integer departmentId,
                 Department department, Integer branchId, Branch branch, Integer roomId, Room room) {
        this.userId = userId;
        this.user = user;
        this.roleId = roleId;
        this.role = role;
        this.departmentId = departmentId;
        this.department = department;
        this.branchId = branchId;
        this.branch = branch;
        this.roomId = roomId;
        this.room = room;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
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
        private Integer userId;
        private User user;
        private Integer roleId;
        private Role role;
        private Integer departmentId;
        private Department department;
        private Integer branchId;
        private Branch branch;
        private Integer roomId;
        private Room room;

        public Builder userId(Integer userId) {
            this.userId = userId;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder roleId(Integer roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder departmentId(Integer departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public Builder department(Department department) {
            this.department = department;
            return this;
        }

        public Builder branchId(Integer branchId) {
            this.branchId = branchId;
            return this;
        }

        public Builder branch(Branch branch) {
            this.branch = branch;
            return this;
        }

        public Builder roomId(Integer roomId) {
            this.roomId = roomId;
            return this;
        }

        public Builder room(Room room) {
            this.room = room;
            return this;
        }

        public Staff build() {
            return new Staff(userId, user, roleId, role, departmentId, department, branchId, branch, roomId, room);
        }
    }
}
