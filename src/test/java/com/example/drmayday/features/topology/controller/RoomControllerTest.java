package com.example.drmayday.features.topology.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.topology.dto.CreateRoomRequest;
import com.example.drmayday.features.topology.dto.EditRoomRequest;
import com.example.drmayday.features.topology.dto.RoomResponse;
import com.example.drmayday.features.topology.service.RoomService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
@ContextConfiguration(classes = RoomControllerTest.TestWebConfiguration.class)
class RoomControllerTest {

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @Import({RoomController.class, GlobalExceptionHandler.class})
    static class TestWebConfiguration {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    private static final String BASE_URL = "/api/v1/rooms";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @Nested
    @DisplayName("POST /api/v1/rooms")
    class CreateRoomTests {

        @Test
        @DisplayName("[Happy Path] Valid request — returns created room")
        void createRoom_validRequest_returnsCreatedRoom() throws Exception {
            when(roomService.createRoom(any(CreateRoomRequest.class)))
                    .thenReturn(room("12", "7", "Phòng 101"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"branch_id":"7","name":"Phòng 101"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.message").value("Tạo thành công"))
                    .andExpect(jsonPath("$.data.room_id").value("12"))
                    .andExpect(jsonPath("$.data.branch_id").value("7"))
                    .andExpect(jsonPath("$.data.branch_name").value("Cơ sở Cầu Giấy"))
                    .andExpect(jsonPath("$.data.name").value("Phòng 101"));

            ArgumentCaptor<CreateRoomRequest> requestCaptor =
                    ArgumentCaptor.forClass(CreateRoomRequest.class);
            verify(roomService).createRoom(requestCaptor.capture());
            assertEquals("7", requestCaptor.getValue().getBranchId());
            assertEquals("Phòng 101", requestCaptor.getValue().getName());
        }

        @Test
        @DisplayName("[Unhappy Path] Missing branch ID — returns validation error without service call")
        void createRoom_missingBranchId_returnsValidationError() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Phòng 101"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"))
                    .andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc"));

            verify(roomService, never()).createRoom(any(CreateRoomRequest.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Duplicate room name — returns conflict response")
        void createRoom_duplicateName_returnsConflict() throws Exception {
            when(roomService.createRoom(any(CreateRoomRequest.class)))
                    .thenThrow(new AppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"branch_id":"7","name":"Phòng 101"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("2104"))
                    .andExpect(jsonPath("$.message").value("Tên phòng khám đã tồn tại trong chi nhánh này"));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void createRoom_serviceThrows_returnsStandardServerError() throws Exception {
            when(roomService.createRoom(any(CreateRoomRequest.class)))
                    .thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"branch_id":"7","name":"Phòng 101"}
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rooms")
    class GetListRoomsTests {

        @Test
        @DisplayName("[Happy Path] Branch filter supplied — binds branch_id and returns matching rooms")
        void getListRooms_branchFilter_returnsMatchingRooms() throws Exception {
            when(roomService.getListRooms(7L))
                    .thenReturn(List.of(room("12", "7", "Phòng 101")));

            mockMvc.perform(get(BASE_URL).param("branch_id", "7"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].room_id").value("12"))
                    .andExpect(jsonPath("$.data[0].branch_id").value("7"))
                    .andExpect(jsonPath("$.data[0].branch_name").value("Cơ sở Cầu Giấy"))
                    .andExpect(jsonPath("$.data[0].name").value("Phòng 101"));

            verify(roomService).getListRooms(7L);
        }

        @Test
        @DisplayName("[Alternate Path] Branch filter omitted — passes null and returns all rooms")
        void getListRooms_filterOmitted_returnsAllRooms() throws Exception {
            when(roomService.getListRooms(null)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data").isEmpty());

            verify(roomService).getListRooms(null);
        }

        @Test
        @DisplayName("[Unhappy Path] branch_id is not numeric — returns parameter type error")
        void getListRooms_invalidBranchId_returnsBadRequest() throws Exception {
            mockMvc.perform(get(BASE_URL).param("branch_id", "not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2002"))
                    .andExpect(jsonPath("$.message").value("Kiểu tham số không hợp lệ"));

            verify(roomService, never()).getListRooms(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Filter references a missing branch — returns not found response")
        void getListRooms_missingBranch_returnsNotFound() throws Exception {
            when(roomService.getListRooms(404L))
                    .thenThrow(new AppException(ErrorCode.BRANCH_NOT_FOUND));

            mockMvc.perform(get(BASE_URL).param("branch_id", "404"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("2101"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy chi nhánh"));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void getListRooms_serviceThrows_returnsStandardServerError() throws Exception {
            when(roomService.getListRooms(null))
                    .thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rooms/{id}")
    class GetRoomDetailsTests {

        @Test
        @DisplayName("[Happy Path] Existing room ID — returns room details")
        void getRoomDetails_existingId_returnsRoom() throws Exception {
            when(roomService.getRoomDetails("12")).thenReturn(room("12", "7", "Phòng 101"));

            mockMvc.perform(get(BASE_URL + "/12"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.room_id").value("12"))
                    .andExpect(jsonPath("$.data.branch_id").value("7"))
                    .andExpect(jsonPath("$.data.name").value("Phòng 101"));

            verify(roomService).getRoomDetails("12");
        }

        @Test
        @DisplayName("[Unhappy Path] Room does not exist — returns not found response")
        void getRoomDetails_missingRoom_returnsNotFound() throws Exception {
            when(roomService.getRoomDetails("404"))
                    .thenThrow(new AppException(ErrorCode.ROOM_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/404"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("2103"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy phòng khám"));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void getRoomDetails_serviceThrows_returnsStandardServerError() throws Exception {
            when(roomService.getRoomDetails("12"))
                    .thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(get(BASE_URL + "/12"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/rooms/{id}")
    class EditRoomTests {

        @Test
        @DisplayName("[Happy Path] Valid partial update — forwards ID and fields and returns updated room")
        void editRoom_validRequest_returnsUpdatedRoom() throws Exception {
            when(roomService.editRoom(eq("12"), any(EditRoomRequest.class)))
                    .thenReturn(room("12", "7", "Phòng mới"));

            mockMvc.perform(put(BASE_URL + "/12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Phòng mới"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.room_id").value("12"))
                    .andExpect(jsonPath("$.data.name").value("Phòng mới"));

            ArgumentCaptor<EditRoomRequest> requestCaptor =
                    ArgumentCaptor.forClass(EditRoomRequest.class);
            verify(roomService).editRoom(eq("12"), requestCaptor.capture());
            assertEquals("Phòng mới", requestCaptor.getValue().getName());
            assertNull(requestCaptor.getValue().getBranchId());
        }

        @Test
        @DisplayName("[Unhappy Path] Name exceeds validation limit — returns bad request without service call")
        void editRoom_nameTooLong_returnsValidationError() throws Exception {
            String longName = "x".repeat(151);

            mockMvc.perform(put(BASE_URL + "/12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + longName + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"))
                    .andExpect(jsonPath("$.message").value("Giá trị tham số không hợp lệ"));

            verify(roomService, never()).editRoom(any(), any(EditRoomRequest.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Service reports room has exam sessions — returns bad request")
        void editRoom_roomHasExamSessions_returnsBadRequest() throws Exception {
            when(roomService.editRoom(eq("12"), any(EditRoomRequest.class)))
                    .thenThrow(new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS));

            mockMvc.perform(put(BASE_URL + "/12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"branch_id":"8"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2105"))
                    .andExpect(jsonPath("$.message").value("Phòng khám đang có phiên khám"));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void editRoom_serviceThrows_returnsStandardServerError() throws Exception {
            when(roomService.editRoom(eq("12"), any(EditRoomRequest.class)))
                    .thenThrow(new IllegalStateException("service unavailable"));

            mockMvc.perform(put(BASE_URL + "/12")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Phòng mới"}
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/rooms/{id}")
    class DeleteRoomTests {

        @Test
        @DisplayName("[Happy Path] Room can be deleted — returns success")
        void deleteRoom_roomCanBeDeleted_returnsSuccess() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/12"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.message").value("Thành công"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(roomService).deleteRoom("12");
        }

        @Test
        @DisplayName("[Unhappy Path] Room has exam sessions — returns bad request")
        void deleteRoom_roomHasExamSessions_returnsBadRequest() throws Exception {
            doThrow(new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS))
                    .when(roomService).deleteRoom("12");

            mockMvc.perform(delete(BASE_URL + "/12"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2105"))
                    .andExpect(jsonPath("$.message").value("Phòng khám đang có phiên khám"));
        }

        @Test
        @DisplayName("[Exception Path] Service throws unexpected exception — returns standard server error")
        void deleteRoom_serviceThrows_returnsStandardServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(roomService).deleteRoom("12");

            mockMvc.perform(delete(BASE_URL + "/12"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"))
                    .andExpect(jsonPath("$.message").value("Lỗi hệ thống nội bộ"));
        }
    }

    private static RoomResponse room(String id, String branchId, String name) {
        return new RoomResponse(id, branchId, "Cơ sở Cầu Giấy", name);
    }
}
