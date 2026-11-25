package com.example.drmayday.common.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EchoController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class EchoController {
        @PostMapping("/echo")
        String echo(@RequestBody Body body) {
            return "ok";
        }
    }

    static class Body {
        @JsonProperty("chi_so")
        public BigDecimal chiSo;
        @JsonProperty("items")
        public List<Item> items;
    }

    static class Item {
        @JsonProperty("so_luong")
        public Integer soLuong;
    }

    @Nested
    @DisplayName("handleHttpMessageNotReadableException")
    class UnreadableBody {

        @Test
        @DisplayName("[Unhappy Path] Field sai kiểu trả về 2003 kèm tên field")
        void topLevelFieldWrongType() throws Exception {
            mockMvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{\"chi_so\": \"cao\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"))
                    .andExpect(jsonPath("$.message").value("Giá trị của trường chi_so không đúng kiểu dữ liệu"));
        }

        @Test
        @DisplayName("[Unhappy Path] Field lồng trong danh sách sai kiểu trả về đường dẫn đầy đủ")
        void nestedFieldWrongType() throws Exception {
            mockMvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"items\": [{\"so_luong\": 1}, {\"so_luong\": \"hai\"}]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"))
                    .andExpect(jsonPath("$.message").value("Giá trị của trường items[1].so_luong không đúng kiểu dữ liệu"));
        }

        @Test
        @DisplayName("[Unhappy Path] JSON sai cú pháp trả về 2003 và báo JSON không hợp lệ")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{\"chi_so\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"))
                    .andExpect(jsonPath("$.message").value("Nội dung request body không phải JSON hợp lệ"));
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu request body trả về 2003 và báo thiếu body")
        void missingBody() throws Exception {
            mockMvc.perform(post("/echo").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"))
                    .andExpect(jsonPath("$.message").value("Thiếu hoặc không đọc được nội dung request body"));
        }
    }
}
