package com.example.drmayday.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI drMayDayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DrMayDay Backend API")
                        .version("v1")
                        .description("""
                                Tài liệu API hệ thống DrMayDay.

                                Đăng nhập qua POST /api/v1/auth/login để lấy access token,
                                rồi bấm Authorize và dán token vào để gọi các API cần xác thực.

                                Tài khoản mẫu do DatabaseSeeder tạo:
                                - Admin: 0900000001
                                - Bệnh nhân: 0912345678
                                """))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
