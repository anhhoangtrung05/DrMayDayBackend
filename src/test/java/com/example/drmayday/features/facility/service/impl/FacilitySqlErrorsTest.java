package com.example.drmayday.features.facility.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Khóa cách nhận diện lỗi database theo SQLState. covers: AC-5
 */
class FacilitySqlErrorsTest {

    private static DataIntegrityViolationException withSqlState(String sqlState) {
        return new DataIntegrityViolationException("lỗi ghi", new SQLException("lỗi gốc", sqlState));
    }

    @Test
    @DisplayName("SQLState 23505 là lỗi trùng khóa, không phải lỗi khóa ngoại")
    void recognizesUniqueViolation() {
        DataIntegrityViolationException ex = withSqlState("23505");

        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isTrue();
        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isFalse();
    }

    @Test
    @DisplayName("SQLState 23503 là lỗi khóa ngoại, không phải lỗi trùng khóa")
    void recognizesForeignKeyViolation() {
        DataIntegrityViolationException ex = withSqlState("23503");

        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isTrue();
        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isFalse();
    }

    @Test
    @DisplayName("SQLState nằm sâu trong chuỗi nguyên nhân vẫn được nhận ra")
    void readsSqlStateFromDeepestCause() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("lỗi ghi",
                new RuntimeException("lớp giữa", new SQLException("lỗi gốc", "23503")));

        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isTrue();
    }

    @Test
    @DisplayName("SQLState khác, ví dụ vi phạm CHECK, không khớp mã nào")
    void ignoresOtherSqlStates() {
        DataIntegrityViolationException ex = withSqlState("23514");

        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isFalse();
        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isFalse();
    }

    @Test
    @DisplayName("Nguyên nhân không phải SQLException thì không khớp mã nào")
    void ignoresNonSqlCauses() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("lỗi ghi", new IllegalStateException("không phải SQL"));

        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isFalse();
        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isFalse();
    }

    @Test
    @DisplayName("Ngoại lệ không có nguyên nhân thì không khớp mã nào")
    void ignoresExceptionWithoutCause() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("lỗi ghi");

        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isFalse();
        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isFalse();
    }
}
