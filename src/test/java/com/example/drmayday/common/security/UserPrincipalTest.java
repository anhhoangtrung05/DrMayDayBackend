package com.example.drmayday.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    private static Set<String> authoritiesOf(UserPrincipal principal) {
        return principal.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Map role benh_nhan sang ca ROLE_BENH_NHAN va ROLE_PATIENT")
    void patientRoleAliases() {
        Set<String> fromVi = authoritiesOf(UserPrincipal.create(2L, "0912345678", "benh_nhan"));
        assertTrue(fromVi.contains("ROLE_BENH_NHAN"));
        assertTrue(fromVi.contains("ROLE_PATIENT"));
        assertFalse(fromVi.contains("ROLE_ADMIN"));

        Set<String> fromEn = authoritiesOf(UserPrincipal.create(2L, "0912345678", "ROLE_patient"));
        assertEquals(fromVi, fromEn);
    }

    @Test
    @DisplayName("Map role bac_si sang ca ROLE_BAC_SI va ROLE_DOCTOR")
    void doctorRoleAliases() {
        Set<String> fromVi = authoritiesOf(UserPrincipal.create(3L, "0912345679", "bac_si"));
        assertTrue(fromVi.contains("ROLE_BAC_SI"));
        assertTrue(fromVi.contains("ROLE_DOCTOR"));
        assertFalse(fromVi.contains("ROLE_ADMIN"));

        Set<String> fromEn = authoritiesOf(UserPrincipal.create(3L, "0912345679", "doctor"));
        assertEquals(fromVi, fromEn);
    }

    @Test
    @DisplayName("Map role dieu_duong va admin/null mac dinh an toan")
    void nurseAdminAndDefaultRoles() {
        Set<String> nurse = authoritiesOf(UserPrincipal.create(4L, "0900000004", "dieu_duong"));
        assertTrue(nurse.contains("ROLE_DIEU_DUONG"));
        assertTrue(nurse.contains("ROLE_NURSE"));

        Set<String> admin = authoritiesOf(UserPrincipal.create(1L, "0900000001", "quan_tri"));
        assertTrue(admin.contains("ROLE_ADMIN"));

        Set<String> fallback = authoritiesOf(UserPrincipal.create(5L, "0900000005", null));
        assertTrue(fallback.contains("ROLE_BENH_NHAN"));
        assertTrue(fallback.contains("ROLE_PATIENT"));
    }
}
