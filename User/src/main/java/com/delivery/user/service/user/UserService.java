package com.delivery.user.service.user;

import com.delivery.user.domain.user.User;
import com.delivery.user.dto.request.CreateUserRequestDto;
import com.delivery.user.dto.request.UpdateUserStatusRequestDto;
import com.delivery.user.dto.response.CreateUserResponseDto;
import com.delivery.user.dto.response.UserDetailResponseDto;
import com.delivery.user.exception.ApiException;
import com.delivery.user.exception.UserErrorCode;
import com.delivery.user.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserAuthorizationService userAuthorizationService;
    private final EmailNormalizer emailNormalizer;

    @Transactional
    public CreateUserResponseDto createUser(CreateUserRequestDto request) {
        String normalizedEmail = emailNormalizer.normalize(request.email());
        User savedUser;

        try {
            savedUser = userRepository.save(User.create(normalizedEmail));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        return CreateUserResponseDto.from(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDetailResponseDto getUser(Long userId) {
        return UserDetailResponseDto.from(findUser(userId));
    }

    @Transactional(readOnly = true)
    public UserDetailResponseDto getUser(Long userId, long authenticatedUserId) {
        userAuthorizationService.requireSelf(authenticatedUserId, userId);
        return getUser(userId);
    }

    @Transactional
    public UserDetailResponseDto updateUserStatus(Long userId, UpdateUserStatusRequestDto request) {
        User user = findUser(userId);
        user.updateStatus(request.status());

        return UserDetailResponseDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDetailResponseDto updateUserStatus(
        Long userId,
        UpdateUserStatusRequestDto request,
        long authenticatedUserId
    ) {
        userAuthorizationService.requireSelf(authenticatedUserId, userId);
        return updateUserStatus(userId, request);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(UserErrorCode.USER_NOT_FOUND));
    }

}
