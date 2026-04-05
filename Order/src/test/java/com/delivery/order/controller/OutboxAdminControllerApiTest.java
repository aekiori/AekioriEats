package com.delivery.order.controller;

import com.delivery.order.TestIdempotencyCacheConfig;
import com.delivery.order.dto.response.OutboxReplayResultDto;
import com.delivery.order.dto.response.OutboxResultDto;
import com.delivery.order.exception.InternalApiKeyFilter;
import com.delivery.order.service.OutboxAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestIdempotencyCacheConfig.class)
class OutboxAdminControllerApiTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private InternalApiKeyFilter internalApiKeyFilter;

    @MockitoBean
    private OutboxAdminService outboxAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(internalApiKeyFilter)
            .build();
    }

    @Test
    void get_outboxes_returns_401_when_internal_api_key_is_missing() throws Exception {
        mockMvc.perform(get("/api/v1/internal/outbox")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INTERNAL_API_UNAUTHORIZED"));
    }

    @Test
    void get_outboxes_returns_results_when_internal_api_key_is_valid() throws Exception {
        when(outboxAdminService.getOutboxes("FAILED")).thenReturn(List.of(
            new OutboxResultDto(1L, "event-001", "ORDER", 1L, "OrderCreated", "FAILED", "1", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/v1/internal/outbox")
                .header("X-Internal-Api-Key", "test-internal-api-key")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventId").value("event-001"));
    }

    @Test
    void replay_failed_outbox_returns_result_when_internal_api_key_is_valid() throws Exception {
        when(outboxAdminService.replayFailedOutbox("event-001")).thenReturn(
            new OutboxReplayResultDto("event-001", "INIT", "outbox.event.ORDER", LocalDateTime.now())
        );

        mockMvc.perform(post("/api/v1/internal/outbox/{eventId}/replay", "event-001")
                .header("X-Internal-Api-Key", "test-internal-api-key")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventId").value("event-001"))
            .andExpect(jsonPath("$.status").value("INIT"));
    }
}
