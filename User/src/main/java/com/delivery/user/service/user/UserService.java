package com.delivery.user.service.user;

import com.delivery.user.domain.user.User;
import com.delivery.user.dto.request.CreateUserDto;
import com.delivery.user.dto.request.UpdateUserStatusDto;
import com.delivery.user.dto.response.CreateUserResultDto;
import com.delivery.user.dto.response.UserDetailResultDto;
import com.delivery.user.exception.ApiException;
import com.delivery.user.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserAuthorizationService userAuthorizationService;

    @Transactional
    public CreateUserResultDto createUser(CreateUserDto request) {
        String normalizedEmail = normalizeEmail(request.email());
        User savedUser;

        try {
            savedUser = userRepository.save(User.create(normalizedEmail));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(
                "USER_EMAIL_ALREADY_EXISTS",
                "Email is already registered.",
                HttpStatus.CONFLICT
            );
        }

        return CreateUserResultDto.from(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDetailResultDto getUser(Long userId) {
        return UserDetailResultDto.from(findUser(userId));
    }

    @Transactional(readOnly = true)
    public UserDetailResultDto getUser(Long userId, long authenticatedUserId) {
        userAuthorizationService.requireSelf(authenticatedUserId, userId);
        return getUser(userId);
    }

    @Transactional
    public UserDetailResultDto updateUserStatus(Long userId, UpdateUserStatusDto request) {
        User user = findUser(userId);
        user.updateStatus(request.status());

        return UserDetailResultDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDetailResultDto updateUserStatus(
        Long userId,
        UpdateUserStatusDto request,
        long authenticatedUserId
    ) {
        userAuthorizationService.requireSelf(authenticatedUserId, userId);
        return updateUserStatus(userId, request);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(
                "USER_NOT_FOUND",
                "User was not found.",
                HttpStatus.NOT_FOUND
            ));
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
