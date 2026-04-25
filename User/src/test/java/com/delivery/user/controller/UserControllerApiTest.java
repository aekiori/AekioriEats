package com.delivery.user.controller;

import com.delivery.user.dto.request.CreateUserRequestDto;
import com.delivery.user.dto.response.CreateUserResponseDto;
import com.delivery.user.service.user.UserService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserControllerApiTest {
    private static final String USER_ID_HEADER = "X-User-Id";
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void get_user_api_returns_200_for_owner() throws Exception {
        CreateUserResponseDto createdUser = userService.createUser(new CreateUserRequestDto("owner@example.com"));

        mockMvc.perform(get("/api/v1/users/{userId}", createdUser.userId())
                .header(USER_ID_HEADER, String.valueOf(createdUser.userId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(createdUser.userId()));
    }

    @Test
    void get_user_api_returns_403_when_request_user_is_not_owner() throws Exception {
        CreateUserResponseDto createdUser = userService.createUser(new CreateUserRequestDto("owner2@example.com"));

        mockMvc.perform(get("/api/v1/users/{userId}", createdUser.userId())
                .header(USER_ID_HEADER, "9999"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN_RESOURCE_ACCESS"));
    }

    @Test
    void get_user_api_returns_401_when_principal_header_is_missing() throws Exception {
        CreateUserResponseDto createdUser = userService.createUser(new CreateUserRequestDto("owner3@example.com"));

        mockMvc.perform(get("/api/v1/users/{userId}", createdUser.userId()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED_PRINCIPAL"));
    }

    @Test
    void update_user_status_api_returns_403_when_request_user_is_not_owner() throws Exception {
        CreateUserResponseDto createdUser = userService.createUser(new CreateUserRequestDto("owner4@example.com"));

        mockMvc.perform(patch("/api/v1/users/{userId}/status", createdUser.userId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(USER_ID_HEADER, "9999")
                .content("""
                    {
                      "status": "LOCKED"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN_RESOURCE_ACCESS"));
    }

}
