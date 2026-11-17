package com.example.drmayday.features.medicalrecord.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Khóa cách nhận diện lỗi database theo SQLState cho hồ sơ bệnh án. covers: AC-2, AC-16
 */
class MedicalRecordSqlErrorsTest {

    private static DataIntegrityViolationException withSqlState(String sqlState) {
        return new DataIntegrityViolationException("lỗi ghi", new SQLException("lỗi gốc", sqlState));
    }

    @Test
    @DisplayName("AC-16: SQLState 23503 là lỗi khóa ngoại, không phải giá trị sai")
    void recognizesForeignKeyViolation() {
        DataIntegrityViolationException ex = withSqlState("23503");

        assertThat(MedicalRecordSqlErrors.isForeignKeyViolation(ex)).isTrue();
        assertThat(MedicalRecordSqlErrors.isInvalidValue(ex)).isFalse();
    }

    @ParameterizedTest(name = "AC-2: SQLState {0} là giá trị sai")
    @ValueSource(strings = {"23514", "22003"})
    void recognizesCheckAndNumericOverflowAsInvalidValue(String sqlState) {
        DataIntegrityViolationException ex = withSqlState(sqlState);

        assertThat(MedicalRecordSqlErrors.isInvalidValue(ex)).isTrue();
        assertThat(MedicalRecordSqlErrors.isForeignKeyViolation(ex)).isFalse();
    }

    @Test
    @DisplayName("SQLState trùng khóa 23505 không khớp loại nào, để lỗi được ném lại")
    void ignoresUniqueViolation() {
        DataIntegrityViolationException ex = withSqlState("23505");

        assertThat(MedicalRecordSqlErrors.isForeignKeyViolation(ex)).isFalse();
        assertThat(MedicalRecordSqlErrors.isInvalidValue(ex)).isFalse();
    }

    @Test
    @DisplayName("Spec 0004 AC-3: SQLState 23505 là trùng UNIQUE, khóa ngoại và CHECK thì không")
    void recognizesUniqueViolation() {
        assertThat(MedicalRecordSqlErrors.isUniqueViolation(withSqlState("23505"))).isTrue();
        assertThat(MedicalRecordSqlErrors.isUniqueViolation(withSqlState("23503"))).isFalse();
        assertThat(MedicalRecordSqlErrors.isUniqueViolation(withSqlState("23514"))).isFalse();
    }

    private static DataIntegrityViolationException foreignKey(String message) {
        return new DataIntegrityViolationException("lỗi ghi", new SQLException(message, "23503"));
    }

    @Test
    @DisplayName("Spec 0004 AC-8: khóa ngoại được nhận theo đúng tên ràng buộc trong message của PostgreSQL")
    void recognizesForeignKeyByConstraintName() {
        DataIntegrityViolationException room = foreignKey(
                "insert or update on table \"phienkham\" violates foreign key constraint \"fk_phienkham_phong\"");

        assertThat(MedicalRecordSqlErrors.isForeignKeyViolationOn(room, "fk_phienkham_phong")).isTrue();
        assertThat(MedicalRecordSqlErrors.isForeignKeyViolationOn(room, "fk_phienkham_bacsi")).isFalse();
    }

    @Test
    @DisplayName("Tên ràng buộc chỉ khớp khi đứng trong dấu nháy, không khớp một phần tên dài hơn")
    void constraintNameMustMatchWholeQuotedName() {
        DataIntegrityViolationException longer = foreignKey("violates foreign key constraint \"fk_phienkham_phong_cu\"");

        assertThat(MedicalRecordSqlErrors.isForeignKeyViolationOn(longer, "fk_phienkham_phong")).isFalse();
    }

    @Test
    @DisplayName("Message có tên ràng buộc nhưng SQLState không phải 23503 thì không phải lỗi khóa ngoại đó")
    void constraintNameWithOtherSqlStateIsNotForeignKey() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("lỗi ghi",
                new SQLException("constraint \"fk_phienkham_phong\"", "23514"));

        assertThat(MedicalRecordSqlErrors.isForeignKeyViolationOn(ex, "fk_phienkham_phong")).isFalse();
    }

    @Test
    @DisplayName("SQLState nằm sâu trong chuỗi nguyên nhân vẫn được nhận ra")
    void readsSqlStateFromDeepestCause() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("lỗi ghi",
                new RuntimeException("lớp giữa", new SQLException("lỗi gốc", "23503")));

        assertThat(MedicalRecordSqlErrors.isForeignKeyViolation(ex)).isTrue();
    }

    @Test
    @DisplayName("Nguyên nhân không phải SQLException thì không khớp loại nào")
    void ignoresNonSqlCauses() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("lỗi ghi", new IllegalStateException("không phải SQL"));

        assertThat(MedicalRecordSqlErrors.isForeignKeyViolation(ex)).isFalse();
        assertThat(MedicalRecordSqlErrors.isInvalidValue(ex)).isFalse();
    }
}
