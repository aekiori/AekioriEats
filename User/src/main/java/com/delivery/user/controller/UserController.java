package com.delivery.user.controller;

import com.delivery.user.dto.request.CreateUserDto;
import com.delivery.user.dto.request.UpdateUserStatusDto;
import com.delivery.user.dto.response.CreateUserResultDto;
import com.delivery.user.dto.response.UserDetailResultDto;
import com.delivery.user.service.user.UserService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<CreateUserResultDto> createUser(@Valid @RequestBody CreateUserDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResultDto> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserDetailResultDto> updateUserStatus(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserStatusDto request
    ) {
        return ResponseEntity.ok(userService.updateUserStatus(userId, request));
    }
}
