package com.delivery.order.controller;

import com.delivery.order.dto.response.OutboxReplayResultDto;
import com.delivery.order.dto.response.OutboxResultDto;
import com.delivery.order.service.OutboxAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/outbox")
public class OutboxAdminController {
    private final OutboxAdminService outboxAdminService;

    @GetMapping
    public ResponseEntity<List<OutboxResultDto>> getOutboxes(@RequestParam(defaultValue = "FAILED") String status) {
        return ResponseEntity.ok(outboxAdminService.getOutboxes(status));
    }

    @PostMapping("/{eventId}/replay")
    public ResponseEntity<OutboxReplayResultDto> replayFailedOutbox(@PathVariable String eventId) {
        return ResponseEntity.ok(outboxAdminService.replayFailedOutbox(eventId));
    }
}
