package com.delivery.user.service;

import com.delivery.user.domain.user.User;
import com.delivery.user.dto.request.CreateUserDto;
import com.delivery.user.dto.response.CreateUserResultDto;
import com.delivery.user.exception.ApiException;
import com.delivery.user.repository.user.UserRepository;
import com.delivery.user.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CreateUserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void create_user_saves_email_and_active_status() {
        CreateUserResultDto result = userService.createUser(
            new CreateUserDto("aekiori@example.com")
        );

        User savedUser = userRepository.findById(result.userId()).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo("aekiori@example.com");
        assertThat(savedUser.getStatus()).isEqualTo(User.Status.ACTIVE);
    }

    @Test
    void create_user_throws_conflict_when_email_is_duplicated() {
        userService.createUser(new CreateUserDto("aekioridup@example.com"));

        assertThatThrownBy(() -> userService.createUser(new CreateUserDto("aekioridup@example.com")))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Email is already registered.");
    }
}
