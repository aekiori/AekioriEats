package com.delivery.auth.controller;

import com.delivery.auth.dto.request.LoginRequestDto;
import com.delivery.auth.dto.request.LogoutRequestDto;
import com.delivery.auth.dto.request.RefreshTokenRequestDto;
import com.delivery.auth.dto.request.SignupRequestDto;
import com.delivery.auth.dto.response.AuthTokenResponseDto;
import com.delivery.auth.dto.response.EmailDuplicateCheckResultDto;
import com.delivery.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @GetMapping("/email/exists")
    public ResponseEntity<EmailDuplicateCheckResultDto> checkEmailDuplicate(@RequestParam @NotBlank @Email String email) {
        return ResponseEntity.ok(authService.checkEmailDuplicate(email));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthTokenResponseDto> signup(@Valid @RequestBody SignupRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequestDto request) {
        authService.logout(request);

        return ResponseEntity.noContent().build();
    }
}
