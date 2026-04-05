package com.delivery.order.controller;

import com.delivery.order.TestIdempotencyCacheConfig;
import com.delivery.order.dto.request.CreateOrderDto;
import com.delivery.order.dto.request.CreateOrderItemDto;
import com.delivery.order.service.order.CreateOrderService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestIdempotencyCacheConfig.class)
class OrderControllerApiTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CreateOrderService createOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void create_order_api_returns_201_and_response_body() throws Exception {
        String requestBody = """
            {
              "userId": 1,
              "storeId": 100,
              "deliveryAddress": "서울시 강남구 테헤란로 123",
              "usedPointAmount": 1000,
              "items": [
                {
                  "menuId": 10,
                  "menuName": "불고기버거",
                  "unitPrice": 8500,
                  "quantity": 2
                },
                {
                  "menuId": 20,
                  "menuName": "콜라",
                  "unitPrice": 2000,
                  "quantity": 1
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", "order-create-api-001")
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").isNumber())
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.storeId").value(100))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalAmount").value(19000))
            .andExpect(jsonPath("$.finalAmount").value(18000));
    }

    @Test
    void create_order_api_returns_400_when_validation_fails() throws Exception {
        String requestBody = """
            {
              "userId": null,
              "storeId": 100,
              "deliveryAddress": "",
              "usedPointAmount": -1,
              "items": []
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", "trace-validation-001")
                .header("X-Idempotency-Key", "order-create-validation-001")
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/orders"))
            .andExpect(jsonPath("$.traceId").value("trace-validation-001"))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void create_order_api_returns_400_when_idempotency_key_header_is_missing() throws Exception {
        String requestBody = """
            {
              "userId": 1,
              "storeId": 100,
              "deliveryAddress": "서울시 강남구 테헤란로 123",
              "usedPointAmount": 1000,
              "items": [
                {
                  "menuId": 10,
                  "menuName": "불고기버거",
                  "unitPrice": 8500,
                  "quantity": 2
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", "trace-header-validation-001")
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.path").value("/api/v1/orders"))
            .andExpect(jsonPath("$.traceId").value("trace-header-validation-001"))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void get_order_api_returns_order_detail() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void get_orders_api_returns_400_when_page_request_is_invalid() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                .param("userId", "1")
                .param("page", "-1")
                .param("size", "0")
                .header("X-Trace-Id", "trace-page-validation-001"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.path").value("/api/v1/orders"))
            .andExpect(jsonPath("$.traceId").value("trace-page-validation-001"))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void get_orders_api_returns_400_when_status_is_invalid() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                .param("userId", "1")
                .param("status", "INVALID")
                .header("X-Trace-Id", "trace-status-query-001"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.path").value("/api/v1/orders"))
            .andExpect(jsonPath("$.traceId").value("trace-status-query-001"))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void update_order_status_api_returns_409_for_invalid_transition() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(patch("/api/v1/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "PAID",
                      "reason": "payment completed"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(patch("/api/v1/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", "trace-status-001")
                .content("""
                    {
                      "status": "FAILED",
                      "reason": "invalid transition"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/orders/" + orderId + "/status"))
            .andExpect(jsonPath("$.traceId").value("trace-status-001"))
            .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS_TRANSITION"))
            .andExpect(jsonPath("$.message", Matchers.containsString("currentStatus=PAID")));
    }

    @Test
    void create_order_api_returns_same_order_for_same_idempotency_key() throws Exception {
        String requestBody = """
            {
              "userId": 1,
              "storeId": 100,
              "deliveryAddress": "서울시 강남구 테헤란로 123",
              "usedPointAmount": 1000,
              "items": [
                {
                  "menuId": 10,
                  "menuName": "불고기버거",
                  "unitPrice": 8500,
                  "quantity": 2
                },
                {
                  "menuId": 20,
                  "menuName": "콜라",
                  "unitPrice": 2000,
                  "quantity": 1
                }
              ]
            }
            """;

        String firstResponse = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", "order-create-001")
                .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", "order-create-001")
                .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        org.assertj.core.api.Assertions.assertThat(firstResponse).isEqualTo(secondResponse);
    }

    @Test
    void create_order_api_returns_409_for_conflicting_idempotency_key_request() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", "order-create-conflict-001")
                .content("""
                    {
                      "userId": 1,
                      "storeId": 100,
                      "deliveryAddress": "서울시 강남구 테헤란로 123",
                      "usedPointAmount": 1000,
                      "items": [
                        {
                          "menuId": 10,
                          "menuName": "불고기버거",
                          "unitPrice": 8500,
                          "quantity": 2
                        }
                      ]
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", "order-create-conflict-001")
                .content("""
                    {
                      "userId": 1,
                      "storeId": 100,
                      "deliveryAddress": "서울시 강남구 테헤란로 999",
                      "usedPointAmount": 1000,
                      "items": [
                        {
                          "menuId": 10,
                          "menuName": "불고기버거",
                          "unitPrice": 8500,
                          "quantity": 2
                        }
                      ]
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    private Long createOrder() {
        return createOrderService.createOrder(
            new CreateOrderDto(
                1L,
                100L,
                "서울시 강남구 테헤란로 123",
                1000,
                List.of(
                    new CreateOrderItemDto(10L, "불고기버거", 8500, 2),
                    new CreateOrderItemDto(20L, "콜라", 2000, 1)
                )
            ),
            "order-create-api-helper-001"
        ).orderId();
    }
}
