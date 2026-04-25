package com.delivery.auth.controller;

import com.delivery.auth.dto.request.LoginRequestDto;
import com.delivery.auth.dto.request.LogoutRequestDto;
import com.delivery.auth.dto.request.RefreshTokenRequestDto;
import com.delivery.auth.dto.request.SignupRequestDto;
import com.delivery.auth.dto.response.AuthTokenResponseDto;
import com.delivery.auth.dto.response.EmailDuplicateCheckResponseDto;
import com.delivery.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@Tag(name = "Auth", description = "회원가입, 로그인, 토큰 발급 API")
public class AuthController {
    private final AuthService authService;

    @GetMapping("/email/exists")
    @Operation(summary = "이메일 중복 확인", description = "가입 가능한 이메일인지 확인한다.")
    public ResponseEntity<EmailDuplicateCheckResponseDto> checkEmailDuplicate(
        @Parameter(description = "중복 확인할 이메일", required = true, example = "order-tester@example.com")
        @RequestParam @NotBlank @Email String email
    ) {
        return ResponseEntity.ok(authService.checkEmailDuplicate(email));
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새 계정을 만들고 access/refresh token을 발급한다.")
    public ResponseEntity<AuthTokenResponseDto> signup(
        @Valid @RequestBody SignupRequestDto request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(authService.signup(request, resolveClientIp(httpServletRequest)));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 access/refresh token을 발급한다.")
    public ResponseEntity<AuthTokenResponseDto> login(
        @Valid @RequestBody LoginRequestDto request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(authService.login(request, resolveClientIp(httpServletRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "refresh token으로 새로운 access/refresh token을 발급한다.")
    public ResponseEntity<AuthTokenResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "refresh token을 무효화한다.")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequestDto request) {
        authService.logout(request);

        return ResponseEntity.noContent().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
