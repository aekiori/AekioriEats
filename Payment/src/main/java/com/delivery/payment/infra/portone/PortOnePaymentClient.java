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

    @Value("${payment.portone.store-id:}")
    private String storeId;

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

        requireApiSecret();

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

    public PortOnePaymentCancellation cancelPayment(String paymentId, Integer amount, String reason) {
        if (!verifyEnabled) {
            return new PortOnePaymentCancellation(paymentId, null, "MOCK_CANCELLED", amount);
        }

        requireApiSecret();
        if (paymentId == null || paymentId.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "PortOne paymentId is required to cancel payment."
            );
        }

        String encodedPaymentId = URLEncoder.encode(paymentId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(trimTrailingSlash(apiBaseUrl) + "/payments/" + encodedPaymentId + "/cancel"))
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "PortOne " + apiSecret)
            .POST(HttpRequest.BodyPublishers.ofString(buildCancelRequestBody(amount, reason)))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "PortOne payment cancel failed. status=" + response.statusCode()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode cancellation = root.path("cancellation");
            if (cancellation.isMissingNode() || cancellation.isNull()) {
                cancellation = root;
            }

            return new PortOnePaymentCancellation(
                paymentId,
                textValue(cancellation, "id", null),
                textValue(cancellation, "status", null),
                amountValue(cancellation)
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne payment cancel failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne payment cancel interrupted.", exception);
        }
    }

    private void requireApiSecret() {
        if (apiSecret == null || apiSecret.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PortOne API secret is required when verification is enabled."
            );
        }
    }

    private String buildCancelRequestBody(Integer amount, String reason) {
        try {
            JsonNode root = objectMapper.createObjectNode();
            if (root instanceof com.fasterxml.jackson.databind.node.ObjectNode objectNode) {
                objectNode.put("amount", amount);
                objectNode.put("reason", normalizeReason(reason));
                objectNode.put("requester", "ADMIN");

                if (storeId != null && !storeId.isBlank()) {
                    objectNode.put("storeId", storeId);
                }
            }

            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PortOne cancel body serialization failed.", exception);
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Store rejected order.";
        }

        return reason;
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
