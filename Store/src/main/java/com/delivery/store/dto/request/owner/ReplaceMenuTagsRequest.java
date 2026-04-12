package com.delivery.store.dto.request.owner;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceMenuTagsRequest(
    @NotNull List<String> tagNames
) {
}
