package com.delivery.store.controller.store;

import com.delivery.store.dto.request.StoreSearchRequestDto;
import com.delivery.store.dto.response.query.StoreQueryResponseDto;
import com.delivery.store.service.store.StoreQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreQueryController {
    private final StoreQueryService storeQueryService;

    @GetMapping({"", "/search"})
    public StoreQueryResponseDto.StoreSearchPageResponseDto searchStores(
        @Valid @ModelAttribute StoreSearchRequestDto request
    ) {
        return storeQueryService.searchStores(request);
    }

    @GetMapping("/{storeId}")
    public StoreQueryResponseDto.StoreDetailResponseDto getStoreDetail(@PathVariable Long storeId) {
        return storeQueryService.getStoreDetail(storeId);
    }
}
