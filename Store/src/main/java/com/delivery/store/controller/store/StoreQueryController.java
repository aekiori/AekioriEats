package com.delivery.store.controller.store;

import com.delivery.store.dto.response.query.StoreQueryDtos;
import com.delivery.store.service.store.StoreQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreQueryController {
    private final StoreQueryService storeQueryService;

    @GetMapping({"", "/search"})
    public StoreQueryDtos.StoreSearchPageDto searchStores(
        @RequestParam(value = "q", defaultValue = "") String query,
        @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return storeQueryService.searchStores(query, page, size);
    }

    @GetMapping("/{storeId}")
    public StoreQueryDtos.StoreDetailDto getStoreDetail(@PathVariable Long storeId) {
        return storeQueryService.getStoreDetail(storeId);
    }
}
