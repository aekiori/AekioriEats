package com.delivery.point.controller.kafkadlt;

import com.delivery.point.dto.request.KafkaDltReadRequestDto;
import com.delivery.point.dto.request.KafkaDltReplayRequestDto;
import com.delivery.point.dto.response.KafkaDltMessageResponseDto;
import com.delivery.point.dto.response.KafkaDltReplayResponseDto;

import com.delivery.point.service.kafkadlt.KafkaDltService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/kafka-dlt")
public class KafkaDltController {
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final KafkaDltService kafkaDltService;

    @Value("${point.internal.api-key:point-internal-dev-key}")
    private String internalApiKey;

    @GetMapping("/messages")
    public ResponseEntity<List<KafkaDltMessageResponseDto>> readMessages(
        @RequestHeader(INTERNAL_API_KEY_HEADER) String apiKey,
        @Valid @ModelAttribute KafkaDltReadRequestDto request
    ) {
        verifyApiKey(apiKey);
        return ResponseEntity.ok(kafkaDltService.readMessages(request));
    }

    @PostMapping("/replay")
    public ResponseEntity<KafkaDltReplayResponseDto> replay(
        @RequestHeader(INTERNAL_API_KEY_HEADER) String apiKey,
        @Valid @RequestBody KafkaDltReplayRequestDto request
    ) {
        verifyApiKey(apiKey);
        return ResponseEntity.ok(kafkaDltService.replay(request));
    }

    private void verifyApiKey(String apiKey) {
        if (!MessageDigest.isEqual(
            internalApiKey.getBytes(StandardCharsets.UTF_8),
            apiKey.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal API key.");
        }
    }
}
