package com.delivery.payment.controller;

import com.delivery.payment.dto.request.ConfirmPaymentRequest;
import com.delivery.payment.dto.response.ConfirmPaymentResponse;
import com.delivery.payment.service.payment.ConfirmPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final ConfirmPaymentService confirmPaymentService;

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmPaymentResponse> confirm(@RequestBody ConfirmPaymentRequest request) {
        return ResponseEntity.ok(confirmPaymentService.confirm(request));
    }
}
