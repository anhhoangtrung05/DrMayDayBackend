package com.example.drmayday.features.rbac.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Permission;
import com.example.drmayday.features.rbac.dto.CreatePermissionRequest;
import com.example.drmayday.features.rbac.dto.PermissionResponse;
import com.example.drmayday.repository.PermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private static void assertError(ErrorCode expected, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }

    @Nested
    @DisplayName("createPermission")
    class CreatePermissionTests {

        @Test
        @DisplayName("[Happy Path] Valid request — creates permission with timestamps and trimmed fields")
        void createPermission_validRequest_createsPermission() {
            when(permissionRepository.existsByName("export_reports")).thenReturn(false);
            when(permissionRepository.saveAndFlush(any(Permission.class))).thenAnswer(invocation -> {
                Permission saved = invocation.getArgument(0);
                saved.setPermissionId(10L);
                return saved;
            });

            PermissionResponse response = permissionService.createPermission(CreatePermissionRequest.builder()
                    .permissionName(" export_reports ")
                    .description(" Xuất báo cáo ")
                    .groupName(" REPORTS ")
                    .build());

            assertThat(response.getPermissionId()).isEqualTo("10");
            assertThat(response.getPermissionName()).isEqualTo("export_reports");
            assertThat(response.getDescription()).isEqualTo("Xuất báo cáo");
            assertThat(response.getGroupName()).isEqualTo("REPORTS");
        }

        @Test
        @DisplayName("[Alternate Path] Blank group_name and description — normalizes to null to satisfy DB CHECK")
        void createPermission_blankOptionalFields_normalizesToNull() {
            when(permissionRepository.existsByName("view_dashboard")).thenReturn(false);
            when(permissionRepository.saveAndFlush(any(Permission.class))).thenAnswer(invocation -> {
                Permission saved = invocation.getArgument(0);
                assertThat(saved.getCreatedAt()).isNotNull();
                assertThat(saved.getUpdatedAt()).isNotNull();
                saved.setPermissionId(11L);
                return saved;
            });

            PermissionResponse response = permissionService.createPermission(CreatePermissionRequest.builder()
                    .permissionName("view_dashboard")
                    .description("   ")
                    .groupName("   ")
                    .build());

            assertThat(response.getPermissionId()).isEqualTo("11");
            assertThat(response.getDescription()).isNull();
            assertThat(response.getGroupName()).isNull();
        }

        @Test
        @DisplayName("[Unhappy Path] Duplicate permission_name — throws 3108")
        void createPermission_duplicate_throwsConflict() {
            when(permissionRepository.existsByName("manage_clinical")).thenReturn(true);

            assertError(ErrorCode.PERMISSION_ALREADY_EXISTS,
                    () -> permissionService.createPermission(CreatePermissionRequest.builder()
                            .permissionName("manage_clinical")
                            .build()));

            verify(permissionRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Missing permission_name — throws 2001")
        void createPermission_blankName_throwsMissingRequired() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> permissionService.createPermission(null));
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> permissionService.createPermission(CreatePermissionRequest.builder().permissionName(" ").build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Name or group exceeds 100 chars — throws 2003")
        void createPermission_tooLong_throwsInvalidParameter() {
            String over100 = "p".repeat(101);
            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> permissionService.createPermission(CreatePermissionRequest.builder().permissionName(over100).build()));
            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> permissionService.createPermission(CreatePermissionRequest.builder()
                            .permissionName("valid")
                            .groupName(over100)
                            .build()));
        }

        @Test
        @DisplayName("[Exception Path] Concurrent duplicate constraint — maps uq_quyen_han_ten to 3108")
        void createPermission_concurrentDuplicate_mapsToConflict() {
            when(permissionRepository.existsByName("export_reports")).thenReturn(false);
            when(permissionRepository.saveAndFlush(any(Permission.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_quyen_han_ten\""));

            assertError(ErrorCode.PERMISSION_ALREADY_EXISTS,
                    () -> permissionService.createPermission(CreatePermissionRequest.builder()
                            .permissionName("export_reports")
                            .build()));
        }
    }

    @Nested
    @DisplayName("getListPermissions")
    class GetListPermissionsTests {

        @Test
        @DisplayName("[Happy Path] Returns all permissions sorted by permissionId ASC")
        void getListPermissions_returnsSortedList() {
            OffsetDateTime now = OffsetDateTime.now();
            when(permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "permissionId"))).thenReturn(List.of(
                    new Permission(1L, "manage_users_v1", "USER_MANAGEMENT", "Quản lý user", now, now),
                    new Permission(2L, "manage_clinical", "CLINICAL", "Quản lý bệnh án", now, now)
            ));

            List<PermissionResponse> result = permissionService.getListPermissions();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPermissionId()).isEqualTo("1");
            assertThat(result.get(0).getPermissionName()).isEqualTo("manage_users_v1");
            assertThat(result.get(1).getPermissionName()).isEqualTo("manage_clinical");
        }
    }
}
