package com.example.drmayday.features.topology.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.topology.dto.BranchResponse;
import com.example.drmayday.features.topology.dto.CreateBranchRequest;
import com.example.drmayday.features.topology.dto.EditBranchRequest;
import com.example.drmayday.features.topology.service.BranchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = BranchControllerTest.TestWebConfiguration.class)
class BranchControllerTest {

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @Import({BranchController.class, GlobalExceptionHandler.class})
    static class TestWebConfiguration {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    private static final String BASE_URL = "/api/v1/branches";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BranchService branchService;

    @Nested
    @DisplayName("POST /api/v1/branches")
    class CreateBranchTests {

        @Test
        @DisplayName("[Happy Path] Valid request — returns created branch")
        void createBranch_validRequest_returnsCreatedBranch() throws Exception {
            when(branchService.createBranch(any(CreateBranchRequest.class)))
                    .thenReturn(branch("7", "Cơ sở Cầu Giấy", true));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Cơ sở Cầu Giấy","address":"Số 2 Duy Tân","phone":"0241234567"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.message").value("Tạo thành công"))
                    .andExpect(jsonPath("$.data.branch_id").value("7"))
                    .andExpect(jsonPath("$.data.name").value("Cơ sở Cầu Giấy"))
                    .andExpect(jsonPath("$.data.address").value("Số 1 Phố Huế"))
                    .andExpect(jsonPath("$.data.phone").value("0241234567"))
                    .andExpect(jsonPath("$.data.status").value(true));

            ArgumentCaptor<CreateBranchRequest> requestCaptor =
                    ArgumentCaptor.forClass(CreateBranchRequest.class);
            verify(branchService).createBranch(requestCaptor.capture());
            assertEquals("Cơ sở Cầu Giấy", requestCaptor.getValue().getName());
            assertEquals("Số 2 Duy Tân", requestCaptor.getValue().getAddress());
            assertEquals("0241234567", requestCaptor.getValue().getPhone());
        }

        @Test
        @DisplayName("[Unhappy Path] Missing required name — returns validation error without service call")
        void createBranch_missingName_returnsValidationError() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"address":"Số 2 Duy Tân","phone":"0241234567"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"))
                    .andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc"));

            verify(branchService, never()).createBranch(any(CreateBranchRequest.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid phone — returns validation error without service call")
        void createBranch_invalidPhone_returnsValidationError() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Cơ sở Cầu Giấy","address":"Số 2 Duy Tân","phone":"phone"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2004"))
                    .andExpect(jsonPath("$.message").value("Định dạng số điện thoại không hợp lệ"));

            verify(branchService, never()).createBranch(any(CreateBranchRequest.class));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void createBranch_serviceThrows_returnsStandardServerError() throws Exception {
            when(branchService.createBranch(any(CreateBranchRequest.class)))
                    .thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Cơ sở Cầu Giấy","address":"Số 2 Duy Tân"}
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/branches")
    class GetListBranchesTests {

        @Test
        @DisplayName("[Happy Path] Branches exist — returns branch list")
        void getListBranches_branchesExist_returnsList() throws Exception {
            when(branchService.getListBranches()).thenReturn(List.of(
                    branch("7", "Cơ sở Cầu Giấy", true),
                    branch("8", "Cơ sở Hoàn Kiếm", false)));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].branch_id").value("7"))
                    .andExpect(jsonPath("$.data[0].name").value("Cơ sở Cầu Giấy"))
                    .andExpect(jsonPath("$.data[0].status").value(true))
                    .andExpect(jsonPath("$.data[1].branch_id").value("8"))
                    .andExpect(jsonPath("$.data[1].status").value(false));

            verify(branchService).getListBranches();
        }

        @Test
        @DisplayName("[Alternate Path] No branches — returns empty list")
        void getListBranches_noBranches_returnsEmptyList() throws Exception {
            when(branchService.getListBranches()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void getListBranches_serviceThrows_returnsStandardServerError() throws Exception {
            when(branchService.getListBranches()).thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/branches/{id}")
    class GetBranchDetailsTests {

        @Test
        @DisplayName("[Happy Path] Existing branch ID — returns branch details")
        void getBranchDetails_existingId_returnsBranch() throws Exception {
            when(branchService.getBranchDetails("7"))
                    .thenReturn(branch("7", "Cơ sở Cầu Giấy", true));

            mockMvc.perform(get(BASE_URL + "/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.branch_id").value("7"))
                    .andExpect(jsonPath("$.data.name").value("Cơ sở Cầu Giấy"));

            verify(branchService).getBranchDetails("7");
        }

        @Test
        @DisplayName("[Unhappy Path] Branch does not exist — returns not found response")
        void getBranchDetails_missingBranch_returnsNotFound() throws Exception {
            when(branchService.getBranchDetails("404"))
                    .thenThrow(new AppException(ErrorCode.BRANCH_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/404"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("2101"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy chi nhánh"));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void getBranchDetails_serviceThrows_returnsStandardServerError() throws Exception {
            when(branchService.getBranchDetails("7"))
                    .thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(get(BASE_URL + "/7"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/branches/{id}")
    class EditBranchTests {

        @Test
        @DisplayName("[Happy Path] Valid partial update — forwards ID and fields and returns updated branch")
        void editBranch_validRequest_updatesBranch() throws Exception {
            when(branchService.editBranch(eq("7"), any(EditBranchRequest.class)))
                    .thenReturn(branch("7", "Cơ sở mới", false));

            mockMvc.perform(put(BASE_URL + "/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Cơ sở mới","status":false}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.branch_id").value("7"))
                    .andExpect(jsonPath("$.data.name").value("Cơ sở mới"))
                    .andExpect(jsonPath("$.data.status").value(false));

            ArgumentCaptor<EditBranchRequest> requestCaptor =
                    ArgumentCaptor.forClass(EditBranchRequest.class);
            verify(branchService).editBranch(eq("7"), requestCaptor.capture());
            assertEquals("Cơ sở mới", requestCaptor.getValue().getName());
            assertEquals(Boolean.FALSE, requestCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid phone — returns validation error without service call")
        void editBranch_invalidPhone_returnsValidationError() throws Exception {
            mockMvc.perform(put(BASE_URL + "/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"phone":"invalid"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2004"));

            verify(branchService, never()).editBranch(any(), any(EditBranchRequest.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Service rejects blank name — returns bad request")
        void editBranch_blankName_returnsBadRequest() throws Exception {
            when(branchService.editBranch(eq("7"), any(EditBranchRequest.class)))
                    .thenThrow(new AppException(ErrorCode.INVALID_PARAMETER_VALUE));

            mockMvc.perform(put(BASE_URL + "/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":" "}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void editBranch_serviceThrows_returnsStandardServerError() throws Exception {
            when(branchService.editBranch(eq("7"), any(EditBranchRequest.class)))
                    .thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(put(BASE_URL + "/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Cơ sở mới"}
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/branches/{id}")
    class DeleteBranchTests {

        @Test
        @DisplayName("[Happy Path] Branch has no dependencies — returns success")
        void deleteBranch_noDependencies_returnsSuccess() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Thành công"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(branchService).deleteBranch("7");
        }

        @Test
        @DisplayName("[Unhappy Path] Branch has dependent rooms or employees — returns bad request")
        void deleteBranch_hasDependencies_returnsBadRequest() throws Exception {
            doThrow(new AppException(ErrorCode.BRANCH_HAS_DEPENDENCIES))
                    .when(branchService).deleteBranch("7");

            mockMvc.perform(delete(BASE_URL + "/7"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2102"))
                    .andExpect(jsonPath("$.message").value("Chi nhánh vẫn còn phòng khám hoặc nhân viên"));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void deleteBranch_serviceThrows_returnsStandardServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(branchService).deleteBranch("7");

            mockMvc.perform(delete(BASE_URL + "/7"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    private static BranchResponse branch(String id, String name, boolean active) {
        return new BranchResponse(id, name, "Số 1 Phố Huế", "0241234567", active);
    }
}
