package com.example.drmayday.features.medicalrecord.service.impl;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

/**
 * Nhận diện lỗi database theo SQLState, không dựa vào tên ràng buộc hay message.
 */
final class MedicalRecordSqlErrors {

    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String CHECK_VIOLATION = "23514";
    private static final String NUMERIC_OUT_OF_RANGE = "22003";

    private MedicalRecordSqlErrors() {
    }

    static boolean isForeignKeyViolation(DataIntegrityViolationException ex) {
        return FOREIGN_KEY_VIOLATION.equals(sqlState(ex));
    }

    // Driver PostgreSQL chỉ có ở runtime, nên nhận diện ràng buộc qua tên trong message của nó
    static boolean isForeignKeyViolationOn(DataIntegrityViolationException ex, String constraint) {
        String message = ex.getMostSpecificCause().getMessage();
        return isForeignKeyViolation(ex) && message != null && message.contains("\"" + constraint + "\"");
    }

    static boolean isUniqueViolation(DataIntegrityViolationException ex) {
        return UNIQUE_VIOLATION.equals(sqlState(ex));
    }

    // Vi phạm CHECK hoặc số vượt giới hạn cột: dữ liệu gửi lên sai, trả 2003
    static boolean isInvalidValue(DataIntegrityViolationException ex) {
        String state = sqlState(ex);
        return CHECK_VIOLATION.equals(state) || NUMERIC_OUT_OF_RANGE.equals(state);
    }

    private static String sqlState(DataIntegrityViolationException ex) {
        return ex.getMostSpecificCause() instanceof SQLException sqlException
                ? sqlException.getSQLState()
                : null;
    }
}
