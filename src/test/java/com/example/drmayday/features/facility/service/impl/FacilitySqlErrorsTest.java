package com.example.drmayday.features.facility.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers database error classification by SQLState (AC-5).
 */
class FacilitySqlErrorsTest {

    private static DataIntegrityViolationException withSqlState(String sqlState) {
        return new DataIntegrityViolationException("lỗi ghi", new SQLException("lỗi gốc", sqlState));
    }

    @Test
    @DisplayName("SQLState 23505 is a unique constraint violation, not a foreign key violation")
    void recognizesUniqueViolation() {
        DataIntegrityViolationException ex = withSqlState("23505");

        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isTrue();
        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isFalse();
    }

    @Test
    @DisplayName("SQLState 23503 is a foreign key violation, not a unique constraint violation")
    void recognizesForeignKeyViolation() {
        DataIntegrityViolationException ex = withSqlState("23503");

        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isTrue();
        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isFalse();
    }

    @Test
    @DisplayName("SQLState is recognized when nested deep in the cause chain")
    void readsSqlStateFromDeepestCause() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("lỗi ghi",
                new RuntimeException("lớp giữa", new SQLException("lỗi gốc", "23503")));

        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isTrue();
    }

    @Test
    @DisplayName("Other SQLState values, such as a CHECK violation, do not match either code")
    void ignoresOtherSqlStates() {
        DataIntegrityViolationException ex = withSqlState("23514");

        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isFalse();
        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isFalse();
    }

    @Test
    @DisplayName("A cause that is not SQLException does not match either code")
    void ignoresNonSqlCauses() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("lỗi ghi", new IllegalStateException("không phải SQL"));

        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isFalse();
        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isFalse();
    }

    @Test
    @DisplayName("An exception with no cause does not match either code")
    void ignoresExceptionWithoutCause() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("lỗi ghi");

        assertThat(FacilitySqlErrors.isUniqueViolation(ex)).isFalse();
        assertThat(FacilitySqlErrors.isForeignKeyViolation(ex)).isFalse();
    }
}
