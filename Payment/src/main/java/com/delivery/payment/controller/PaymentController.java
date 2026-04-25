package com.delivery.payment.controller;

import com.delivery.payment.auth.AuthenticatedUser;
import com.delivery.payment.auth.AuthenticatedUserInfo;
import com.delivery.payment.dto.request.ConfirmPaymentRequest;
import com.delivery.payment.dto.response.ConfirmPaymentResponse;
import com.delivery.payment.service.payment.ConfirmPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payment", description = "결제 확인 및 후속 처리 API")
public class PaymentController {
    private final ConfirmPaymentService confirmPaymentService;

    @PostMapping("/confirm")
    @Operation(summary = "결제 confirm", description = "클라이언트 결제 완료 후 결제 검증 및 성공 처리를 수행한다.")
    public ResponseEntity<ConfirmPaymentResponse> confirm(
        @RequestBody ConfirmPaymentRequest request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(confirmPaymentService.confirm(request, authenticatedUser.userId()));
    }
}
