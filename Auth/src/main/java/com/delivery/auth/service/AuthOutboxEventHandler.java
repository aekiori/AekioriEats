package com.delivery.auth.service;

import com.delivery.auth.domain.user.event.UserCreatedOutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuthOutboxEventHandler {
    private final AuthOutboxService authOutboxService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(UserCreatedOutboxEvent event) {
        authOutboxService.saveUserCreated(event);
    }
}
