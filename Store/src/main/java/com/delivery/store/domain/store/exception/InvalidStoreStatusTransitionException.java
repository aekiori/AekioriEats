package com.delivery.store.domain.store.exception;

import com.delivery.store.domain.store.Store;

public class InvalidStoreStatusTransitionException extends RuntimeException {
    public InvalidStoreStatusTransitionException(Store.Status currentStatus, Store.Status targetStatus) {
        super("Cannot change store status from %s to %s.".formatted(currentStatus, targetStatus));
    }
}
