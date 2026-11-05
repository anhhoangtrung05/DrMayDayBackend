package com.example.drmayday.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VaiTroQuyenHanId implements Serializable {

    private Long maVaiTro;
    private Long maQuyenHan;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaiTroQuyenHanId that = (VaiTroQuyenHanId) o;
        return Objects.equals(maVaiTro, that.maVaiTro) && Objects.equals(maQuyenHan, that.maQuyenHan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maVaiTro, maQuyenHan);
    }
}
