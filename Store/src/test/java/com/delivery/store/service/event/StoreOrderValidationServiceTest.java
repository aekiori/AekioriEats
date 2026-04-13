package com.delivery.store.service.event;

import com.delivery.store.constant.OrderEventType;
import com.delivery.store.domain.store.Store;
import com.delivery.store.dto.event.OrderCreatedEventDto;
import com.delivery.store.repository.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreOrderValidationServiceTest {
    @Mock
    private StoreRepository storeRepository;

    private StoreOrderableValidator storeOrderValidationService;

    @BeforeEach
    void setUp() {
        storeOrderValidationService = new StoreOrderableValidator(storeRepository);
    }

    @Test
    void validate_rejects_when_store_not_found() {
        OrderCreatedEventDto event = createEvent(100L, 20000);

        when(storeRepository.findById(100L)).thenReturn(Optional.empty());

        StoreOrderValidationResult result = storeOrderValidationService.validate(event);

        assertThat(result.accepted()).isFalse();
        assertThat(result.code()).isEqualTo("STORE_NOT_FOUND");
    }

    @Test
    void validate_rejects_when_store_is_not_open() {
        Store store = Store.create(1L, "Closed Store");
        store.updateStatus(Store.Status.CLOSED);
        OrderCreatedEventDto event = createEvent(100L, 20000);

        when(storeRepository.findById(100L)).thenReturn(Optional.of(store));

        StoreOrderValidationResult result = storeOrderValidationService.validate(event);

        assertThat(result.accepted()).isFalse();
        assertThat(result.code()).isEqualTo("STORE_NOT_OPEN");
    }

    @Test
    void validate_rejects_when_total_amount_is_less_than_minimum() {
        Store store = Store.create(1L, "Open Store");
        store.updateDeliveryPolicy(15000, 2000);
        OrderCreatedEventDto event = createEvent(100L, 14000);

        when(storeRepository.findById(100L)).thenReturn(Optional.of(store));

        StoreOrderValidationResult result = storeOrderValidationService.validate(event);

        assertThat(result.accepted()).isFalse();
        assertThat(result.code()).isEqualTo("MIN_ORDER_AMOUNT_NOT_MET");
    }

    @Test
    void validate_accepts_when_store_is_open_and_minimum_is_satisfied() {
        Store store = Store.create(1L, "Open Store");
        store.updateDeliveryPolicy(15000, 2000);
        OrderCreatedEventDto event = createEvent(100L, 15000);

        when(storeRepository.findById(100L)).thenReturn(Optional.of(store));

        StoreOrderValidationResult result = storeOrderValidationService.validate(event);

        assertThat(result.accepted()).isTrue();
        assertThat(result.code()).isEqualTo("OK");
    }

    private OrderCreatedEventDto createEvent(Long storeId, Integer totalAmount) {
        return new OrderCreatedEventDto(
            "event-001",
            OrderEventType.ORDER_CREATED,
            1,
            LocalDateTime.parse("2026-04-12T09:00:00"),
            10L,
            20L,
            storeId,
            totalAmount,
            0,
            totalAmount,
            "PENDING"
        );
    }
}
