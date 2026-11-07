package com.example.drmayday.features.authentication.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.features.authentication.config.SpeedSmsProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Component
public class SpeedSmsClient implements PasswordResetSmsSender {

    private static final Logger log = LoggerFactory.getLogger(SpeedSmsClient.class);

    private final RestClient restClient;
    private final SpeedSmsProperties properties;

    public SpeedSmsClient(RestClient.Builder restClientBuilder, SpeedSmsProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public void send(String phone, String code, long expirationMinutes) {
        if (properties.apiToken().isBlank()) {
            log.error("SpeedSMS is not configured: SPEEDSMS_API_TOKEN is empty");
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "SMS provider is not configured");
        }

        SpeedSmsRequest request = new SpeedSmsRequest(
                List.of(toInternationalPhone(phone)),
                String.format(Locale.ROOT, properties.passwordResetMessage(), code, expirationMinutes),
                properties.smsType(),
                properties.sender().isBlank() ? null : properties.sender()
        );
        String credentials = Base64.getEncoder()
                .encodeToString((properties.apiToken() + ":x").getBytes(StandardCharsets.UTF_8));

        try {
            SpeedSmsResponse response = restClient.post()
                    .uri(properties.apiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Basic " + credentials)
                    .body(request)
                    .retrieve()
                    .body(SpeedSmsResponse.class);

            if (response == null || !"success".equalsIgnoreCase(response.status())) {
                String providerCode = response != null ? response.code() : "empty response";
                log.error("SpeedSMS rejected password reset SMS (code={})", providerCode);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Unable to send password reset SMS");
            }
        } catch (RestClientException ex) {
            log.error("SpeedSMS request failed: {}", ex.getMessage());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Unable to send password reset SMS");
        }
    }

    private String toInternationalPhone(String phone) {
        if (phone.startsWith("+84")) {
            return phone.substring(1);
        }
        if (phone.startsWith("0")) {
            return "84" + phone.substring(1);
        }
        return phone;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SpeedSmsRequest(List<String> to, String content, int sms_type, String sender) {
    }

    private record SpeedSmsResponse(String status, String code, String message) {
    }
}
