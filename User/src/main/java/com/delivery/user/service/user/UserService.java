package com.delivery.user.service.user;

import com.delivery.user.domain.user.User;
import com.delivery.user.dto.request.CreateUserDto;
import com.delivery.user.dto.request.UpdateUserStatusDto;
import com.delivery.user.dto.response.CreateUserResultDto;
import com.delivery.user.dto.response.EmailDuplicateCheckResultDto;
import com.delivery.user.dto.response.UserDetailResultDto;
import com.delivery.user.exception.ApiException;
import com.delivery.user.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEmailBloomFilter userEmailBloomFilter;

    @Transactional(readOnly = true)
    public EmailDuplicateCheckResultDto checkEmailDuplicate(String email) {
        String normalizedEmail = normalizeEmail(email);
        /**
         * note -- 이메일 중복검사는 캐싱을 둘까? 얼마나?
         * 차피 index range scan 만 해서 ㅈㄴ 간단할거같은데 굳이 redis 들락날락거릴 필요 없을수도.
         */
        if (!userEmailBloomFilter.shouldCheckDb(normalizedEmail)) {
            return EmailDuplicateCheckResultDto.from(normalizedEmail, false);
        }

        boolean exists = userRepository.existsByEmail(normalizedEmail);

        return EmailDuplicateCheckResultDto.from(normalizedEmail, exists);
    }

    @Transactional
    public CreateUserResultDto createUser(CreateUserDto request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userEmailBloomFilter.shouldCheckDb(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(
                "USER_EMAIL_ALREADY_EXISTS",
                "Email is already registered.",
                HttpStatus.CONFLICT
            );
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User savedUser;

        try {
            savedUser = userRepository.save(User.create(normalizedEmail, encodedPassword));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(
                "USER_EMAIL_ALREADY_EXISTS",
                "Email is already registered.",
                HttpStatus.CONFLICT
            );
        }

        userEmailBloomFilter.put(normalizedEmail);

        return CreateUserResultDto.from(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDetailResultDto getUser(Long userId) {
        return UserDetailResultDto.from(findUser(userId));
    }

    @Transactional
    public UserDetailResultDto updateUserStatus(Long userId, UpdateUserStatusDto request) {
        User user = findUser(userId);
        user.updateStatus(request.status());

        return UserDetailResultDto.from(userRepository.save(user));
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
