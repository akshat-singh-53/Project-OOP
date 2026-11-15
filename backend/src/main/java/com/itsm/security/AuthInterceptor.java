package com.itsm.security;

import com.itsm.domain.user.User;
import com.itsm.exception.AuthenticationException;
import com.itsm.exception.UnauthorizedOperationException;
import com.itsm.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * Runs before every /api request: validates the bearer token, loads the user and enforces
 * any {@link RequiresRole} restriction declared on the target controller.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTRIBUTE = "itsm.currentUser";
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository users;

    public AuthInterceptor(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || "OPTIONS".equals(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            throw AuthenticationException.missingToken();
        }
        Long userId = jwtService.verify(header.substring(BEARER.length()).trim());
        User user = users.findById(userId).orElseThrow(AuthenticationException::invalidToken);
        if (!user.isActive()) {
            throw AuthenticationException.accountDisabled();
        }
        request.setAttribute(CURRENT_USER_ATTRIBUTE, user);

        RequiresRole rule = method.getMethodAnnotation(RequiresRole.class);
        if (rule == null) {
            rule = method.getBeanType().getAnnotation(RequiresRole.class);
        }
        if (rule != null && Arrays.stream(rule.value()).noneMatch(r -> r == user.getRole())) {
            throw new UnauthorizedOperationException("Your role (" + user.getRole() + ") cannot perform this operation");
        }
        return true;
    }
}
