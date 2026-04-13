package com.delivery.store.service.event;

import com.delivery.store.domain.store.StoreOrderValidation;
import com.delivery.store.dto.event.OrderCreatedEventDto;
import com.delivery.store.repository.store.StoreOrderValidationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreOrderValidationHistoryServiceTest {
    @Mock
    private StoreOrderValidationRepository storeOrderValidationRepository;

    private StoreOrderValidationHistoryService storeOrderValidationHistoryService;

    @BeforeEach
    void setUp() {
        storeOrderValidationHistoryService = new StoreOrderValidationHistoryService(storeOrderValidationRepository);
        when(storeOrderValidationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void save_creates_accepted_history_when_order_has_no_previous_record() {
        OrderCreatedEventDto event = createEvent(1L, 100L);
        when(storeOrderValidationRepository.findTopByOrderIdOrderByIdDesc(1L)).thenReturn(Optional.empty());

        StoreOrderValidation saved = storeOrderValidationHistoryService.save(
            event,
            StoreOrderValidationResult.pass()
        );

        assertThat(saved.getOrderId()).isEqualTo(1L);
        assertThat(saved.getStoreId()).isEqualTo(100L);
        assertThat(saved.getResult()).isEqualTo(StoreOrderValidation.Result.ACCEPTED);
        assertThat(saved.getRejectCode()).isNull();
        assertThat(saved.getRejectReason()).isNull();
        assertThat(saved.getValidatedAt()).isNotNull();
    }

    @Test
    void save_creates_rejected_history_when_order_has_no_previous_record() {
        OrderCreatedEventDto event = createEvent(2L, 200L);
        when(storeOrderValidationRepository.findTopByOrderIdOrderByIdDesc(2L)).thenReturn(Optional.empty());

        StoreOrderValidation saved = storeOrderValidationHistoryService.save(
            event,
            StoreOrderValidationResult.reject("MIN_ORDER_AMOUNT_NOT_MET", "totalAmount=12000, minOrderAmount=15000")
        );

        assertThat(saved.getResult()).isEqualTo(StoreOrderValidation.Result.REJECTED);
        assertThat(saved.getRejectCode()).isEqualTo("MIN_ORDER_AMOUNT_NOT_MET");
        assertThat(saved.getRejectReason()).isEqualTo("totalAmount=12000, minOrderAmount=15000");
        assertThat(saved.getValidatedAt()).isNotNull();
    }

    @Test
    void save_updates_existing_history_record() {
        OrderCreatedEventDto event = createEvent(3L, 300L);
        StoreOrderValidation existing = StoreOrderValidation.createRejected(
            3L,
            300L,
            "STORE_NOT_OPEN",
            "Store is closed",
            LocalDateTime.parse("2026-04-12T10:00:00")
        );
        when(storeOrderValidationRepository.findTopByOrderIdOrderByIdDesc(3L)).thenReturn(Optional.of(existing));

        StoreOrderValidation saved = storeOrderValidationHistoryService.save(
            event,
            StoreOrderValidationResult.pass()
        );

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getResult()).isEqualTo(StoreOrderValidation.Result.ACCEPTED);
        assertThat(saved.getRejectCode()).isNull();
        assertThat(saved.getRejectReason()).isNull();
    }

    private OrderCreatedEventDto createEvent(Long orderId, Long storeId) {
        return new OrderCreatedEventDto(
            "event-" + orderId,
            "OrderCreated",
            1,
            LocalDateTime.parse("2026-04-12T09:00:00"),
            orderId,
            20L,
            storeId,
            20000,
            0,
            20000,
            "PENDING"
        );
    }
}
