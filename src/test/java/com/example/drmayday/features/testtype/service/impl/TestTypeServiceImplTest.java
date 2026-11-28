package com.example.drmayday.features.testtype.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.ProvocationTestType;
import com.example.drmayday.features.testtype.dto.CreateTestTypeRequest;
import com.example.drmayday.features.testtype.dto.TestTypeResponse;
import com.example.drmayday.features.testtype.dto.UpdateTestTypeRequest;
import com.example.drmayday.repository.ProvocationTestResultRepository;
import com.example.drmayday.repository.ProvocationTestTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestTypeServiceImplTest {

    @Mock
    private ProvocationTestTypeRepository testTypeRepository;

    @Mock
    private ProvocationTestResultRepository testResultRepository;

    @InjectMocks
    private TestTypeServiceImpl testTypeService;

    @Nested
    @DisplayName("API 104: createTestType")
    class CreateTestTypeTests {

        @Test
        @DisplayName("[Happy Path] Trims name, defaults la_can_nguyen_khac=false, saves and returns DTO")
        void createTestType_success() {
            when(testTypeRepository.existsByTestName("Test áp lực")).thenReturn(false);
            when(testTypeRepository.saveAndFlush(any(ProvocationTestType.class))).thenAnswer(inv -> {
                ProvocationTestType saved = inv.getArgument(0);
                saved.setProvocationTestId(10L);
                return saved;
            });

            TestTypeResponse res = testTypeService.createTestType(CreateTestTypeRequest.builder()
                    .tenNghiemPhap("  Test áp lực  ")
                    .phuongPhapThucHien("  Treo tạ 7kg  ")
                    .thoiGianChuan(15)
                    .build());

            assertEquals(10L, res.getMaNghiemPhap());
            assertEquals("Test áp lực", res.getTenNghiemPhap());
            assertEquals("Treo tạ 7kg", res.getPhuongPhapThucHien());
            assertEquals(15, res.getThoiGianChuan());
            assertFalse(res.getLaCanNguyenKhac());
        }

        @Test
        @DisplayName("[Unhappy Path] Blank name throws 2001")
        void createTestType_blankName_throws2001() {
            AppException ex = assertThrows(AppException.class, () ->
                    testTypeService.createTestType(CreateTestTypeRequest.builder().tenNghiemPhap("   ").build()));
            assertEquals(ErrorCode.MISSING_REQUIRED_PARAMETER, ex.getErrorCode());
        }

        @Test
        @DisplayName("[Unhappy Path] Name > 150 chars or thoi_gian_chuan <= 0 throws 2003")
        void createTestType_invalidValues_throws2003() {
            AppException exName = assertThrows(AppException.class, () ->
                    testTypeService.createTestType(CreateTestTypeRequest.builder()
                            .tenNghiemPhap("a".repeat(151)).build()));
            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exName.getErrorCode());

            AppException exDuration = assertThrows(AppException.class, () ->
                    testTypeService.createTestType(CreateTestTypeRequest.builder()
                            .tenNghiemPhap("Test lạnh").thoiGianChuan(0).build()));
            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exDuration.getErrorCode());
        }

        @Test
        @DisplayName("[Unhappy Path] Duplicate trimmed name throws 8302")
        void createTestType_duplicate_throws8302() {
            when(testTypeRepository.existsByTestName("Da vẽ nổi")).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () ->
                    testTypeService.createTestType(CreateTestTypeRequest.builder()
                            .tenNghiemPhap(" Da vẽ nổi ").build()));
            assertEquals(ErrorCode.TEST_TYPE_ALREADY_EXISTS, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("API 105-106: getTestTypes & getTestType")
    class ReadTestTypeTests {

        @Test
        @DisplayName("Lists test types ordered by ID")
        void getTestTypes_success() {
            when(testTypeRepository.findAllByOrderByProvocationTestIdAsc()).thenReturn(List.of(
                    ProvocationTestType.builder().provocationTestId(1L).testName("Da vẽ nổi").otherCause(false).build(),
                    ProvocationTestType.builder().provocationTestId(9L).testName("Căn nguyên khác").otherCause(true).build()
            ));

            List<TestTypeResponse> res = testTypeService.getTestTypes();
            assertEquals(2, res.size());
            assertEquals(1L, res.get(0).getMaNghiemPhap());
            assertTrue(res.get(1).getLaCanNguyenKhac());
        }

        @Test
        @DisplayName("Get detail throws 8301 when absent")
        void getTestType_notFound_throws8301() {
            when(testTypeRepository.findById(99L)).thenReturn(Optional.empty());
            AppException ex = assertThrows(AppException.class, () -> testTypeService.getTestType(99L));
            assertEquals(ErrorCode.TEST_TYPE_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("API 107-108: updateTestType & deleteTestType")
    class UpdateDeleteTests {

        @Test
        @DisplayName("Update blocks changing la_can_nguyen_khac when results reference it (8303)")
        void updateTestType_inUseFlagChange_throws8303() {
            ProvocationTestType existing = ProvocationTestType.builder()
                    .provocationTestId(1L).testName("Da vẽ nổi").otherCause(false)
                    .createdAt(OffsetDateTime.now().minusDays(1)).updatedAt(OffsetDateTime.now().minusDays(1))
                    .build();
            when(testTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(testTypeRepository.existsByTestNameAndProvocationTestIdNot("Da vẽ nổi", 1L)).thenReturn(false);
            when(testResultRepository.existsByProvocationTestTypeId(1L)).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () ->
                    testTypeService.updateTestType(1L, UpdateTestTypeRequest.builder()
                            .tenNghiemPhap("Da vẽ nổi")
                            .laCanNguyenKhac(true)
                            .build()));
            assertEquals(ErrorCode.TEST_TYPE_IN_USE, ex.getErrorCode());
        }

        @Test
        @DisplayName("Update succeeds with valid values")
        void updateTestType_success() {
            ProvocationTestType existing = ProvocationTestType.builder()
                    .provocationTestId(1L).testName("Da vẽ nổi").otherCause(false)
                    .createdAt(OffsetDateTime.now().minusDays(1)).updatedAt(OffsetDateTime.now().minusDays(1))
                    .build();
            when(testTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(testTypeRepository.existsByTestNameAndProvocationTestIdNot("Da vẽ nổi Fric", 1L)).thenReturn(false);
            when(testTypeRepository.saveAndFlush(any(ProvocationTestType.class))).thenAnswer(inv -> inv.getArgument(0));

            TestTypeResponse res = testTypeService.updateTestType(1L, UpdateTestTypeRequest.builder()
                    .tenNghiemPhap("Da vẽ nổi Fric")
                    .phuongPhapThucHien("FricTest 4.0")
                    .thoiGianChuan(10)
                    .build());

            assertEquals("Da vẽ nổi Fric", res.getTenNghiemPhap());
            assertEquals(10, res.getThoiGianChuan());
            assertEquals(BigDecimal.valueOf(10), existing.getStandardDuration());
        }

        @Test
        @DisplayName("Delete blocks when referenced (8303), deletes when unused")
        void deleteTestType_behavior() {
            ProvocationTestType existing = ProvocationTestType.builder().provocationTestId(1L).build();
            when(testTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(testResultRepository.existsByProvocationTestTypeId(1L)).thenReturn(true);

            AppException ex = assertThrows(AppException.class, () -> testTypeService.deleteTestType(1L));
            assertEquals(ErrorCode.TEST_TYPE_IN_USE, ex.getErrorCode());
            verify(testTypeRepository, never()).delete(any());

            when(testResultRepository.existsByProvocationTestTypeId(1L)).thenReturn(false);
            testTypeService.deleteTestType(1L);
            verify(testTypeRepository).delete(existing);
            verify(testTypeRepository).flush();
        }
    }
}
