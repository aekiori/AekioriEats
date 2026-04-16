package com.delivery.payment.infra.portone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PortOnePaymentClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    @Value("${payment.portone.api-base-url:https://api.portone.io}")
    private String apiBaseUrl;

    @Value("${payment.portone.api-secret:}")
    private String apiSecret;

    @Value("${payment.portone.verify-enabled:false}")
    private boolean verifyEnabled;

    public boolean isVerifyEnabled() {
        return verifyEnabled;
    }

    public PortOnePaymentVerification getPayment(String paymentId) {
        if (!verifyEnabled) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PortOne verification is disabled."
            );
        }

        if (apiSecret == null || apiSecret.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PortOne API secret is required when verification is enabled."
            );
        }

        String encodedPaymentId = URLEncoder.encode(paymentId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(trimTrailingSlash(apiBaseUrl) + "/payments/" + encodedPaymentId))
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json")
            .header("Authorization", "PortOne " + apiSecret)
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "PortOne payment lookup failed. status=" + response.statusCode()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            return new PortOnePaymentVerification(
                textValue(root, "id", paymentId),
                textValue(root, "status", null),
                amountValue(root),
                textValue(root, "currency", null)
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne payment lookup failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne payment lookup interrupted.", exception);
        }
    }

    private String textValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return defaultValue;
        }
        return valueNode.asText();
    }

    private Integer amountValue(JsonNode root) {
        JsonNode total = root.path("amount").path("total");
        if (total.isNumber()) {
            return total.asInt();
        }

        JsonNode plainAmount = root.path("amount");
        if (plainAmount.isNumber()) {
            return plainAmount.asInt();
        }

        return null;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://api.portone.io";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
