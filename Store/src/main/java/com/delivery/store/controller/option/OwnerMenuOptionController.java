package com.delivery.store.controller.option;

import com.delivery.store.auth.AuthenticatedUser;
import com.delivery.store.auth.AuthenticatedUserInfo;
import com.delivery.store.dto.request.owner.ReplaceMenuOptionGroupsRequestDto;
import com.delivery.store.dto.response.ReplaceMenuOptionGroupsResponseDto;
import com.delivery.store.service.option.MenuOptionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/owner/stores/{storeId}/menus/{menuId}/option-groups")
public class OwnerMenuOptionController {
    private final MenuOptionService menuOptionService;

    @PutMapping
    public ResponseEntity<ReplaceMenuOptionGroupsResponseDto> replaceMenuOptionGroups(
        @PathVariable Long storeId,
        @PathVariable Long menuId,
        @Valid @RequestBody ReplaceMenuOptionGroupsRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            menuOptionService.replaceMenuOptionGroups(
                storeId,
                menuId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            )
        );
    }
}
