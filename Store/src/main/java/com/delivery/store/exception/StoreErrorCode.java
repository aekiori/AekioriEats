package com.delivery.store.exception;

import org.springframework.http.HttpStatus;

public enum StoreErrorCode {
    STORE_NOT_FOUND("Store was not found.", HttpStatus.NOT_FOUND),
    MENU_NOT_FOUND("Menu was not found.", HttpStatus.NOT_FOUND),
    MENU_GROUP_NOT_FOUND("Menu group was not found.", HttpStatus.NOT_FOUND),
    MENU_GROUP_NOT_FOUND_FOR_STORE(
        "Menu group was not found for this store.",
        HttpStatus.BAD_REQUEST
    ),
    STORE_ORDER_NOT_FOUND("Store order was not found.", HttpStatus.NOT_FOUND),
    STORE_ORDER_MISMATCH("Store order does not belong to this store.", HttpStatus.BAD_REQUEST),
    STORE_ORDER_ALREADY_DECIDED(
        "Store order decision is already completed.",
        HttpStatus.CONFLICT
    ),
    INVALID_STORE_HOURS("Store hours are invalid.", HttpStatus.BAD_REQUEST),
    INVALID_CATEGORY_IDS("One or more categories were not found.", HttpStatus.BAD_REQUEST),
    STORE_NAME_ALREADY_EXISTS_FOR_OWNER(
        "Store name is already in use for this owner.",
        HttpStatus.CONFLICT
    ),
    UNAUTHORIZED_PRINCIPAL(
        "Authenticated principal is missing or invalid.",
        HttpStatus.UNAUTHORIZED
    ),
    FORBIDDEN_RESOURCE_ACCESS(
        "You do not have permission to access this resource.",
        HttpStatus.FORBIDDEN
    ),
    OUTBOX_PAYLOAD_SERIALIZATION_ERROR(
        "Outbox payload serialization failed.",
        HttpStatus.INTERNAL_SERVER_ERROR
    ),
    TEST_BAD_REQUEST("Intentional bad request for metrics test.", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus status;

    StoreErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }

    public String code() {
        return name();
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}
