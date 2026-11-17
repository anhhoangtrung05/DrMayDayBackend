package com.example.drmayday.features.medicalrecord;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Bác sĩ dùng chung cho các test hồ sơ và phiên khám.
 * DatabaseSeeder chỉ tạo admin và bệnh nhân mẫu, nên test tự tạo nguoi, nhanvien và taikhoan vai trò bac_si.
 * Tìm theo ten_dang_nhap trước: lớp test commit dùng lại đúng một bác sĩ thay vì tạo thêm mỗi lần chạy.
 */
final class TestDoctor {

    static final String PHONE = "0900000006";

    private TestDoctor() {
    }

    /** Trả ma_tai_khoan của bác sĩ test, tạo mới nếu chưa có. */
    static long ensureAccount(JdbcTemplate jdbc) {
        List<Long> existing = jdbc.queryForList(
                "SELECT ma_tai_khoan FROM taikhoan WHERE ten_dang_nhap = ?", Long.class, PHONE);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        List<Long> people = jdbc.queryForList("SELECT ma_nguoi FROM nguoi WHERE so_dien_thoai = ?", Long.class, PHONE);
        long person = people.isEmpty()
                ? jdbc.queryForObject("INSERT INTO nguoi (ho_ten, so_dien_thoai) VALUES ('Bác sĩ Test', ?) RETURNING ma_nguoi",
                        Long.class, PHONE)
                : people.getFirst();
        Integer employees = jdbc.queryForObject("SELECT count(*) FROM nhanvien WHERE ma_nhan_vien = ?", Integer.class, person);
        if (employees == null || employees == 0) {
            List<Long> branches = jdbc.queryForList("SELECT ma_chi_nhanh FROM chinhanh ORDER BY ma_chi_nhanh LIMIT 1", Long.class);
            long branch = branches.isEmpty()
                    ? jdbc.queryForObject("INSERT INTO chinhanh (ten_chi_nhanh, dia_chi) VALUES ('Chi nhánh Test', 'Hà Nội') "
                            + "RETURNING ma_chi_nhanh", Long.class)
                    : branches.getFirst();
            jdbc.update("INSERT INTO nhanvien (ma_nhan_vien, ma_chi_nhanh) VALUES (?, ?)", person, branch);
        }
        return jdbc.queryForObject("""
                INSERT INTO taikhoan (ma_nguoi, ma_vai_tro, ten_dang_nhap, mat_khau_ma_hoa)
                SELECT ?, ma_vai_tro, ?, 'x' FROM vaitro WHERE ten_vai_tro = 'bac_si' RETURNING ma_tai_khoan
                """, Long.class, person, PHONE);
    }
}
