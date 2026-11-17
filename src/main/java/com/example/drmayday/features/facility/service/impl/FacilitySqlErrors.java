package com.example.drmayday.features.facility.service.impl;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

/**
 * Identifies database errors by SQLState. Constraint names are not used because PostgreSQL generates
 * the names for phongkham constraints.
 */
final class FacilitySqlErrors {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private FacilitySqlErrors() {
    }

    static boolean isUniqueViolation(DataIntegrityViolationException ex) {
        return UNIQUE_VIOLATION.equals(sqlState(ex));
    }

    static boolean isForeignKeyViolation(DataIntegrityViolationException ex) {
        return FOREIGN_KEY_VIOLATION.equals(sqlState(ex));
    }

    private static String sqlState(DataIntegrityViolationException ex) {
        return ex.getMostSpecificCause() instanceof SQLException sqlException
                ? sqlException.getSQLState()
                : null;
    }
}
