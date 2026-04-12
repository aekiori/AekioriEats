package com.delivery.store.controller.store;

import com.delivery.store.auth.AuthenticatedUser;
import com.delivery.store.auth.AuthenticatedUserInfo;
import com.delivery.store.dto.request.UpdateStoreStatusDto;
import com.delivery.store.dto.request.owner.CreateOwnerStoreRequest;
import com.delivery.store.dto.request.owner.DeliveryPolicyRequest;
import com.delivery.store.dto.request.owner.ReplaceStoreHolidaysRequest;
import com.delivery.store.dto.request.owner.ReplaceStoreHoursRequest;
import com.delivery.store.dto.request.owner.UpdateOwnerStoreRequest;
import com.delivery.store.dto.response.CreateStoreResultDto;
import com.delivery.store.dto.response.OwnerStoreSummaryResultDto;
import com.delivery.store.dto.response.StoreDetailResultDto;
import com.delivery.store.service.store.StoreScheduleService;
import com.delivery.store.service.store.StoreService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public ResponseEntity<List<OwnerStoreSummaryResultDto>> getOwnerStores(
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(storeService.getOwnerStores(authenticatedUser.userId()));
    }

    @PostMapping
    public ResponseEntity<CreateStoreResultDto> createStore(
        @Valid @RequestBody CreateOwnerStoreRequest request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(storeService.createOwnerStore(request, authenticatedUser.userId()));
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<StoreDetailResultDto> getStore(
        @PathVariable Long storeId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeService.getStore(storeId, authenticatedUser.userId(), authenticatedUser.userRole())
        );
    }

    @PutMapping("/{storeId}")
    public ResponseEntity<StoreDetailResultDto> updateStore(
        @PathVariable Long storeId,
        @Valid @RequestBody UpdateOwnerStoreRequest request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeService.updateOwnerStore(
                storeId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            )
        );
    }

    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(
        @PathVariable Long storeId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        storeService.deleteOwnerStore(storeId, authenticatedUser.userId(), authenticatedUser.userRole());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{storeId}/status")
    public ResponseEntity<StoreDetailResultDto> updateStoreStatus(
        @PathVariable Long storeId,
        @Valid @RequestBody UpdateStoreStatusDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeService.updateStoreStatus(
                storeId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            )
        );
    }

    @PutMapping("/{storeId}/delivery-policy")
    public ResponseEntity<StoreDetailResultDto> replaceDeliveryPolicy(
        @PathVariable Long storeId,
        @Valid @RequestBody DeliveryPolicyRequest request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            storeService.replaceDeliveryPolicy(
                storeId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            )
        );
    }

    @PutMapping("/{storeId}/hours")
    public ResponseEntity<StoreDetailResultDto> replaceStoreHours(
        @PathVariable Long storeId,
        @Valid @RequestBody ReplaceStoreHoursRequest request,
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
    public ResponseEntity<StoreDetailResultDto> replaceStoreHolidays(
        @PathVariable Long storeId,
        @Valid @RequestBody ReplaceStoreHolidaysRequest request,
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
}
