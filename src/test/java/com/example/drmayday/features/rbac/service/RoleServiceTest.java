package com.example.drmayday.features.rbac.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Role;
import com.example.drmayday.features.rbac.dto.CreateRoleRequest;
import com.example.drmayday.features.rbac.dto.RoleResponse;
import com.example.drmayday.repository.RoleRepository;
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
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private static void assertError(ErrorCode expected, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }

    @Nested
    @DisplayName("createRole")
    class CreateRoleTests {

        @Test
        @DisplayName("[Happy Path] Explicit display_name — creates role and returns mapped response")
        void createRole_withDisplayName_createsRole() {
            when(roleRepository.existsByName("duoc_si")).thenReturn(false);
            when(roleRepository.saveAndFlush(any(Role.class))).thenAnswer(invocation -> {
                Role saved = invocation.getArgument(0);
                saved.setRoleId(5L);
                return saved;
            });

            RoleResponse response = roleService.createRole(CreateRoleRequest.builder()
                    .roleName(" duoc_si ")
                    .displayName(" Dược sĩ ")
                    .description(" Quản lý thuốc ")
                    .build());

            assertThat(response.getRoleId()).isEqualTo("5");
            assertThat(response.getRoleName()).isEqualTo("duoc_si");
            assertThat(response.getDisplayName()).isEqualTo("Dược sĩ");
            assertThat(response.getDescription()).isEqualTo("Quản lý thuốc");
        }

        @Test
        @DisplayName("[Alternate Path] Omitted display_name — defaults display_name to role_name")
        void createRole_withoutDisplayName_defaultsToRoleName() {
            when(roleRepository.existsByName("ke_toan")).thenReturn(false);
            when(roleRepository.saveAndFlush(any(Role.class))).thenAnswer(invocation -> {
                Role saved = invocation.getArgument(0);
                saved.setRoleId(6L);
                return saved;
            });

            RoleResponse response = roleService.createRole(CreateRoleRequest.builder()
                    .roleName("ke_toan")
                    .description("   ")
                    .build());

            assertThat(response.getRoleId()).isEqualTo("6");
            assertThat(response.getRoleName()).isEqualTo("ke_toan");
            assertThat(response.getDisplayName()).isEqualTo("ke_toan");
            assertThat(response.getDescription()).isNull();
        }

        @Test
        @DisplayName("[Unhappy Path] Duplicate role_name — throws 3107")
        void createRole_duplicateName_throwsConflict() {
            when(roleRepository.existsByName("admin")).thenReturn(true);

            assertError(ErrorCode.ROLE_ALREADY_EXISTS,
                    () -> roleService.createRole(CreateRoleRequest.builder().roleName("admin").build()));

            verify(roleRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Blank role_name — throws 2001")
        void createRole_blankName_throwsMissingRequired() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> roleService.createRole(null));
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> roleService.createRole(CreateRoleRequest.builder().roleName("   ").build()));
        }

        @Test
        @DisplayName("[Unhappy Path] role_name or display_name exceeds length — throws 2003")
        void createRole_tooLong_throwsInvalidParameter() {
            String over50 = "a".repeat(51);
            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> roleService.createRole(CreateRoleRequest.builder().roleName(over50).build()));

            String over100 = "b".repeat(101);
            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> roleService.createRole(CreateRoleRequest.builder().roleName("valid").displayName(over100).build()));
        }

        @Test
        @DisplayName("[Exception Path] Concurrent duplicate constraint — maps uq_vai_tro_ten to 3107")
        void createRole_concurrentDuplicate_mapsToConflict() {
            when(roleRepository.existsByName("duoc_si")).thenReturn(false);
            when(roleRepository.saveAndFlush(any(Role.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_vai_tro_ten\""));

            assertError(ErrorCode.ROLE_ALREADY_EXISTS,
                    () -> roleService.createRole(CreateRoleRequest.builder().roleName("duoc_si").build()));
        }
    }

    @Nested
    @DisplayName("getRoles")
    class GetRolesTests {

        @Test
        @DisplayName("[Happy Path] Returns all roles sorted by roleId ASC")
        void getRoles_returnsSortedRoles() {
            OffsetDateTime now = OffsetDateTime.now();
            when(roleRepository.findAll(Sort.by(Sort.Direction.ASC, "roleId"))).thenReturn(List.of(
                    new Role(1L, "admin", "Quản trị viên", "Toàn quyền", now, now),
                    new Role(2L, "bac_si", "Bác sĩ", "Khám bệnh", now, now)
            ));

            List<RoleResponse> roles = roleService.getRoles();

            assertThat(roles).hasSize(2);
            assertThat(roles.get(0).getRoleId()).isEqualTo("1");
            assertThat(roles.get(0).getRoleName()).isEqualTo("admin");
            assertThat(roles.get(1).getRoleName()).isEqualTo("bac_si");
        }
    }
}
