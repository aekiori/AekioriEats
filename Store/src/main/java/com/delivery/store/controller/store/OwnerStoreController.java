package com.delivery.store.controller.store;

import com.delivery.store.auth.AuthenticatedUser;
import com.delivery.store.auth.AuthenticatedUserInfo;
import com.delivery.store.dto.request.UpdateStoreStatusRequestDto;
import com.delivery.store.dto.request.owner.CreateOwnerStoreRequestDto;
import com.delivery.store.dto.request.owner.DecideStoreOrderRequestDto;
import com.delivery.store.dto.request.owner.DeliveryPolicyRequestDto;
import com.delivery.store.dto.request.owner.ReplaceStoreHolidaysRequestDto;
import com.delivery.store.dto.request.owner.ReplaceStoreHoursRequestDto;
import com.delivery.store.dto.request.owner.UpdateOwnerStoreRequestDto;
import com.delivery.store.dto.response.CreateStoreResponseDto;
import com.delivery.store.dto.response.OwnerStoreSummaryResponseDto;
import com.delivery.store.dto.response.StoreOrderDecisionResponseDto;
import com.delivery.store.dto.response.StoreOrderResponseDto;
import com.delivery.store.dto.response.StoreDetailResponseDto;
import com.delivery.store.service.store.StoreScheduleService;
import com.delivery.store.service.store.StoreService;
import com.delivery.store.service.store.StoreOrderDecisionService;
import com.delivery.store.dto.request.owner.GetStoreOrdersRequestDto;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/owner/stores")
public class OwnerStoreController {
    private final StoreService storeService;
    private final StoreScheduleService storeScheduleService;
    private final StoreOrderDecisionService storeOrderDecisionService;

    @GetMapping
    public ResponseEntity<List<OwnerStoreSummaryResponseDto>> getOwnerStores(
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(storeService.getOwnerStores(authenticatedUser.userId()));
    }

    @PostMapping
    public ResponseEntity<CreateStoreResponseDto> createStore(
        @Valid @RequestBody CreateOwnerStoreRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(storeService.createOwnerStore(request, authenticatedUser.userId()));
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<StoreDetailResponseDto> getStore(
        @PathVariable Long storeId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeService.getStore(storeId, authenticatedUser.userId())
        );
    }

    @PutMapping("/{storeId}")
    public ResponseEntity<StoreDetailResponseDto> updateStore(
        @PathVariable Long storeId,
        @Valid @RequestBody UpdateOwnerStoreRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeService.updateOwnerStore(
                storeId,
                request,
                authenticatedUser.userId()
            )
        );
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(
        @PathVariable Long storeId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        storeService.deleteOwnerStore(storeId, authenticatedUser.userId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{storeId}/status")
    public ResponseEntity<StoreDetailResponseDto> updateStoreStatus(
        @PathVariable Long storeId,
        @Valid @RequestBody UpdateStoreStatusRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeService.updateStoreStatus(
                storeId,
                request,
                authenticatedUser.userId()
            )
        );
    }

    @PutMapping("/{storeId}/delivery-policy")
    public ResponseEntity<StoreDetailResponseDto> replaceDeliveryPolicy(
        @PathVariable Long storeId,
        @Valid @RequestBody DeliveryPolicyRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeService.replaceDeliveryPolicy(
                storeId,
                request,
                authenticatedUser.userId()
            )
        );
    }

    @PutMapping("/{storeId}/hours")
    public ResponseEntity<StoreDetailResponseDto> replaceStoreHours(
        @PathVariable Long storeId,
        @Valid @RequestBody ReplaceStoreHoursRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeScheduleService.replaceStoreHours(
                storeId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            )
        );
    }

    @PutMapping("/{storeId}/holidays")
    public ResponseEntity<StoreDetailResponseDto> replaceStoreHolidays(
        @PathVariable Long storeId,
        @Valid @RequestBody ReplaceStoreHolidaysRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeScheduleService.replaceStoreHolidays(
                storeId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            )
        );
    }

    @GetMapping("/{storeId}/orders")
    public ResponseEntity<List<StoreOrderResponseDto>> getStoreOrders(
        @PathVariable Long storeId,
        @Valid @ModelAttribute GetStoreOrdersRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeOrderDecisionService.getStoreOrders(
                storeId,
                request,
                authenticatedUser.userId()
            )
        );
    }

    @PostMapping("/{storeId}/orders/{orderId}/decision")
    public ResponseEntity<StoreOrderDecisionResponseDto> decideStoreOrder(
        @PathVariable Long storeId,
        @PathVariable Long orderId,
        @Valid @RequestBody DecideStoreOrderRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeOrderDecisionService.decide(
                storeId,
                orderId,
                request,
                authenticatedUser.userId()
            )
        );
    }
}
