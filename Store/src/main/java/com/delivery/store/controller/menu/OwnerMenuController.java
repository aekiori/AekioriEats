package com.delivery.store.controller.menu;

import com.delivery.store.auth.AuthenticatedUser;
import com.delivery.store.auth.AuthenticatedUserInfo;
import com.delivery.store.dto.request.owner.CreateMenuGroupRequest;
import com.delivery.store.dto.request.owner.CreateMenuRequest;
import com.delivery.store.dto.request.owner.ReplaceMenuTagsRequest;
import com.delivery.store.dto.request.owner.UpdateMenuGroupRequest;
import com.delivery.store.dto.request.owner.UpdateMenuRequest;
import com.delivery.store.dto.response.MenuGroupResultDto;
import com.delivery.store.dto.response.MenuResultDto;
import com.delivery.store.dto.response.ReplaceMenuTagsResultDto;
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
    public ResponseEntity<MenuGroupResultDto> createMenuGroup(
        @PathVariable Long storeId,
        @Valid @RequestBody CreateMenuGroupRequest request,
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
    public ResponseEntity<MenuGroupResultDto> updateMenuGroup(
        @PathVariable Long storeId,
        @PathVariable Long menuGroupId,
        @Valid @RequestBody UpdateMenuGroupRequest request,
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
    public ResponseEntity<MenuResultDto> createMenu(
        @PathVariable Long storeId,
        @Valid @RequestBody CreateMenuRequest request,
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
    public ResponseEntity<MenuResultDto> updateMenu(
        @PathVariable Long storeId,
        @PathVariable Long menuId,
        @Valid @RequestBody UpdateMenuRequest request,
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
    public ResponseEntity<ReplaceMenuTagsResultDto> replaceMenuTags(
        @PathVariable Long storeId,
        @PathVariable Long menuId,
        @Valid @RequestBody ReplaceMenuTagsRequest request,
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
