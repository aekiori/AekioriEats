package com.delivery.user.auth;

import com.delivery.user.service.user.UserAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final UserAuthorizationService userAuthorizationService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedUser.class)
            && parameter.getParameterType().equals(AuthenticatedUserInfo.class);
    }

    @Override
    public Object resolveArgument(
        @NonNull MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        @NonNull NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        long userId = userAuthorizationService.parseAuthenticatedUserId(webRequest.getHeader(USER_ID_HEADER));
        String userRole = normalizeRole(webRequest.getHeader(USER_ROLE_HEADER));
        return new AuthenticatedUserInfo(userId, userRole);
    }

    private String normalizeRole(String rawUserRoleHeader) {
        if (rawUserRoleHeader == null || rawUserRoleHeader.isBlank()) {
            return null;
        }
        return rawUserRoleHeader.trim();
    }
}
