package com.delivery.store.controller.menu;

import com.delivery.store.auth.AuthenticatedUser;
import com.delivery.store.auth.AuthenticatedUserInfo;
import com.delivery.store.dto.request.owner.CreateMenuGroupRequestDto;
import com.delivery.store.dto.request.owner.CreateMenuRequestDto;
import com.delivery.store.dto.request.owner.ReplaceMenuTagsRequestDto;
import com.delivery.store.dto.request.owner.UpdateMenuGroupRequestDto;
import com.delivery.store.dto.request.owner.UpdateMenuRequestDto;
import com.delivery.store.dto.response.MenuGroupResponseDto;
import com.delivery.store.dto.response.MenuResponseDto;
import com.delivery.store.dto.response.ReplaceMenuTagsResponseDto;
import com.delivery.store.service.menu.MenuService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/owner/stores/{storeId}")
public class OwnerMenuController {
    private final MenuService menuService;

    @PostMapping("/menu-groups")
    public ResponseEntity<MenuGroupResponseDto> createMenuGroup(
        @PathVariable Long storeId,
        @Valid @RequestBody CreateMenuGroupRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(menuService.createMenuGroup(
                storeId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            ));
    }

    @PutMapping("/menu-groups/{menuGroupId}")
    public ResponseEntity<MenuGroupResponseDto> updateMenuGroup(
        @PathVariable Long storeId,
        @PathVariable Long menuGroupId,
        @Valid @RequestBody UpdateMenuGroupRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            menuService.updateMenuGroup(
                storeId,
                menuGroupId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            )
        );
    }

    @DeleteMapping("/menu-groups/{menuGroupId}")
    public ResponseEntity<Void> deleteMenuGroup(
        @PathVariable Long storeId,
        @PathVariable Long menuGroupId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        menuService.deleteMenuGroup(
            storeId,
            menuGroupId,
            authenticatedUser.userId(),
            authenticatedUser.userRole()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/menus")
    public ResponseEntity<MenuResponseDto> createMenu(
        @PathVariable Long storeId,
        @Valid @RequestBody CreateMenuRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(menuService.createMenu(
                storeId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            ));
    }

    @PutMapping("/menus/{menuId}")
    public ResponseEntity<MenuResponseDto> updateMenu(
        @PathVariable Long storeId,
        @PathVariable Long menuId,
        @Valid @RequestBody UpdateMenuRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            menuService.updateMenu(
                storeId,
                menuId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            )
        );
    }

    @DeleteMapping("/menus/{menuId}")
    public ResponseEntity<Void> deleteMenu(
        @PathVariable Long storeId,
        @PathVariable Long menuId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        menuService.deleteMenu(storeId, menuId, authenticatedUser.userId(), authenticatedUser.userRole());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/menus/{menuId}/tags")
    public ResponseEntity<ReplaceMenuTagsResponseDto> replaceMenuTags(
        @PathVariable Long storeId,
        @PathVariable Long menuId,
        @Valid @RequestBody ReplaceMenuTagsRequestDto request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            menuService.replaceMenuTags(
                storeId,
                menuId,
                request,
                authenticatedUser.userId(),
                authenticatedUser.userRole()
            )
        );
    }
}
