package com.delivery.store.controller.category;

import com.delivery.store.dto.response.query.StoreQueryDtos;
import com.delivery.store.service.store.StoreQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores/categories")
public class StoreCategoryController {
    private final StoreQueryService storeQueryService;

    @GetMapping
    public List<StoreQueryDtos.CategoryDto> getCategories() {
        return storeQueryService.getCategories();
    }
}
