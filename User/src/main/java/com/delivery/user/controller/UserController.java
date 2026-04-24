package com.delivery.user.controller;

import com.delivery.user.dto.request.CreateUserDto;
import com.delivery.user.dto.request.UpdateUserStatusDto;
import com.delivery.user.dto.response.CreateUserResultDto;
import com.delivery.user.dto.response.UserDetailResultDto;
import com.delivery.user.service.user.UserAuthorizationService;
import com.delivery.user.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 생성, 조회, 상태 변경 API")
public class UserController {
    private final UserAuthorizationService userAuthorizationService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "사용자 생성", description = "User 도메인 사용자를 생성한다.")
    public ResponseEntity<CreateUserResultDto> createUser(@Valid @RequestBody CreateUserDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 상세 조회", description = "본인 사용자 상세를 조회한다.")
    @Parameters({
        @Parameter(name = "userId", description = "조회할 사용자 ID", required = true, example = "1"),
        @Parameter(
            name = "X-User-Id",
            in = ParameterIn.HEADER,
            description = "Gateway가 주입하는 사용자 ID 헤더",
            required = true,
            example = "1"
        )
    })
    public ResponseEntity<UserDetailResultDto> getUser(
        @PathVariable Long userId,
        @RequestHeader(value = "X-User-Id", required = true)
        String authenticatedUserIdHeader
    ) {
        long authenticatedUserId = userAuthorizationService.parseAuthenticatedUserId(authenticatedUserIdHeader);
        return ResponseEntity.ok(userService.getUser(userId, authenticatedUserId));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "사용자 상태 변경", description = "본인 사용자 상태를 변경한다.")
    @Parameters({
        @Parameter(name = "userId", description = "상태를 변경할 사용자 ID", required = true, example = "1"),
        @Parameter(
            name = "X-User-Id",
            in = ParameterIn.HEADER,
            description = "Gateway가 주입하는 사용자 ID 헤더",
            required = true,
            example = "1"
        )
    })
    public ResponseEntity<UserDetailResultDto> updateUserStatus(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserStatusDto request,
        @RequestHeader(value = "X-User-Id", required = true)
        String authenticatedUserIdHeader
    ) {
        long authenticatedUserId = userAuthorizationService.parseAuthenticatedUserId(authenticatedUserIdHeader);
        return ResponseEntity.ok(userService.updateUserStatus(userId, request, authenticatedUserId));
    }
}
