package com.delivery.store.controller;

import com.delivery.store.dto.request.CreateStoreDto;
import com.delivery.store.service.store.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StoreControllerApiTest {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private StoreService storeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void get_store_api_returns_200_for_owner() throws Exception {
        long ownerUserId = 101L;
        long storeId = storeService.createStore(
                new CreateStoreDto(ownerUserId, "Owner Store"),
                ownerUserId,
                "USER"
            )
            .storeId();

        mockMvc.perform(get("/api/v1/owner/stores/{storeId}", storeId)
                .header(USER_ID_HEADER, String.valueOf(ownerUserId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.storeId").value(storeId));
    }

    @Test
    void get_store_api_returns_403_when_request_user_is_not_owner() throws Exception {
        long ownerUserId = 201L;
        long storeId = storeService.createStore(
                new CreateStoreDto(ownerUserId, "Owner Store 2"),
                ownerUserId,
                "USER"
            )
            .storeId();

        mockMvc.perform(get("/api/v1/owner/stores/{storeId}", storeId)
                .header(USER_ID_HEADER, "9999"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN_RESOURCE_ACCESS"));
    }

    @Test
    void get_store_api_returns_401_when_principal_header_is_missing() throws Exception {
        long ownerUserId = 301L;
        long storeId = storeService.createStore(
                new CreateStoreDto(ownerUserId, "Owner Store 3"),
                ownerUserId,
                "USER"
            )
            .storeId();

        mockMvc.perform(get("/api/v1/owner/stores/{storeId}", storeId))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED_PRINCIPAL"));
    }

    @Test
    void update_store_status_api_returns_200_for_admin_role() throws Exception {
        long ownerUserId = 401L;
        long storeId = storeService.createStore(
                new CreateStoreDto(ownerUserId, "Owner Store 4"),
                ownerUserId,
                "USER"
            )
            .storeId();

        mockMvc.perform(patch("/api/v1/owner/stores/{storeId}/status", storeId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(USER_ID_HEADER, "9999")
                .header(USER_ROLE_HEADER, "ADMIN")
                .content("""
                    {
                      "status": "CLOSED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void replace_store_hours_api_accepts_day_of_week_7() throws Exception {
        long ownerUserId = 501L;
        long storeId = storeService.createStore(
                new CreateStoreDto(ownerUserId, "Owner Store 5"),
                ownerUserId,
                "USER"
            )
            .storeId();

        mockMvc.perform(put("/api/v1/owner/stores/{storeId}/hours", storeId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(USER_ID_HEADER, String.valueOf(ownerUserId))
                .content("""
                    {
                      "weeklyHours": [
                        {
                          "dayOfWeek": 7,
                          "openTime": null,
                          "closeTime": null
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void replace_store_hours_api_rejects_day_of_week_0() throws Exception {
        long ownerUserId = 601L;
        long storeId = storeService.createStore(
                new CreateStoreDto(ownerUserId, "Owner Store 6"),
                ownerUserId,
                "USER"
            )
            .storeId();

        mockMvc.perform(put("/api/v1/owner/stores/{storeId}/hours", storeId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(USER_ID_HEADER, String.valueOf(ownerUserId))
                .content("""
                    {
                      "weeklyHours": [
                        {
                          "dayOfWeek": 0,
                          "openTime": null,
                          "closeTime": null
                        }
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
