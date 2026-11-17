package com.example.drmayday.common.exception;

import com.example.drmayday.features.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử standard lỗi HTTP của framework (spec 0005) qua filter chain thật: 405 2007 kèm Allow, 404 2006, 415 2008,
 * và thứ tự bảo mật (401, 403 vẫn trả trước). Mỗi test được rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class FrameworkErrorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String patientToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtTokenProvider.generateToken(1L, "0900000001", "admin");
        patientToken = jwtTokenProvider.generateToken(2L, "0912345678", "benh_nhan");
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(builder);
    }

    private static List<String> allowOf(ResultActions result) {
        String allow = result.andReturn().getResponse().getHeader("Allow");
        assertThat(allow).as("header Allow").isNotNull();
        return List.of(allow.split(",\\s*"));
    }

    @Test
    @DisplayName("405: sai method trả 2007, Allow chứa đúng các method của route, message không lộ chi tiết")
    void wrongMethodIs405() throws Exception {
        ResultActions result = send(patch("/api/v1/branches/1"), adminToken)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("2007"))
                .andExpect(jsonPath("$.message").value(ErrorCode.METHOD_NOT_ALLOWED.getMessage()));
        assertThat(allowOf(result)).contains("GET", "PUT", "DELETE").doesNotContain("PATCH");
        assertThat(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain("PATCH");
    }

    @Test
    @DisplayName("404: route không tồn tại trả 2006, message không lộ đường dẫn")
    void unknownRouteIs404() throws Exception {
        ResultActions result = send(get("/api/v1/khong-ton-tai"), adminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("2006"))
                .andExpect(jsonPath("$.message").value(ErrorCode.RESOURCE_NOT_FOUND.getMessage()));
        assertThat(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain("khong-ton-tai");
    }

    @Test
    @DisplayName("404: route không tồn tại dưới đường dẫn công khai trả 2006 cả khi không có token")
    void unknownPublicRouteIs404WithoutToken() throws Exception {
        send(get("/api/v1/auth/khong-ton-tai"), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("2006"));
    }

    @Test
    @DisplayName("415: Content-Type sai hoặc thiếu khi có body trả 2008")
    void unsupportedMediaTypeIs415() throws Exception {
        send(post("/api/v1/exams").contentType(MediaType.TEXT_PLAIN).content("x"), adminToken)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("2008"))
                .andExpect(jsonPath("$.message").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getMessage()));
        send(post("/api/v1/exams").content("{\"record_id\":\"1\"}"), adminToken)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("2008"));
    }

    @Test
    @DisplayName("Thiếu body trên endpoint cần body vẫn trả 400 2003 như cũ")
    void missingBodyStays2003() throws Exception {
        send(post("/api/v1/exams").contentType(MediaType.APPLICATION_JSON), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));
    }

    @Test
    @DisplayName("Bảo mật trước: không token tới route không tồn tại hoặc method sai vẫn trả 401")
    void noTokenStays401() throws Exception {
        send(get("/api/v1/khong-ton-tai"), null).andExpect(status().isUnauthorized());
        send(delete("/api/v1/exams/1"), null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bảo mật trước: vai trò bị luật đường dẫn chặn nhận 403, không khớp luật nào thì nhận 405")
    void pathRulesDecideBeforeRouteErrors() throws Exception {
        // POST /exams/1 không có route, nhưng khớp luật POST /api/v1/exams/** chỉ cho ADMIN, BAC_SI
        send(post("/api/v1/exams/1"), patientToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3102"));
        send(get("/api/v1/admin/khong-co"), patientToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3102"));
        send(patch("/api/v1/exams/1"), patientToken)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("2007"))
                .andExpect(header().exists("Allow"));
    }

    @Test
    @DisplayName("Lỗi framework phía người gọi ghi WARN một dòng, không còn log ERROR của handler bắt mọi lỗi")
    void loggedAsWarning(CapturedOutput output) throws Exception {
        send(delete("/api/v1/exams/1"), adminToken).andExpect(status().isMethodNotAllowed());
        send(get("/api/v1/khong-ton-tai"), adminToken).andExpect(status().isNotFound());
        assertThat(output.getOut())
                .contains("WARN")
                .contains("Sai HTTP method")
                .contains("Không có route khớp đường dẫn")
                .doesNotContain("Lỗi hệ thống chưa được kiểm soát");
    }
}
