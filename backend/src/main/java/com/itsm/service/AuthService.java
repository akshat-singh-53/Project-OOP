package com.itsm.service;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.exception.AuthenticationException;
import com.itsm.repository.UserRepository;
import com.itsm.security.JwtService;
import com.itsm.security.PasswordService;
import com.itsm.web.dto.UserDtos.AuthResponse;
import com.itsm.web.dto.UserDtos.CreateUserRequest;
import com.itsm.web.dto.UserDtos.LoginRequest;
import com.itsm.web.dto.UserDtos.RegisterRequest;
import com.itsm.web.dto.UserDtos.UserDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordService passwords;
    private final JwtService jwt;
    private final UserService userService;
    private final AuditService audit;

    public AuthService(UserRepository users, PasswordService passwords, JwtService jwt, UserService userService,
                       AuditService audit) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.userService = userService;
        this.audit = audit;
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(AuthenticationException::invalidLogin);
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            audit.recordAdminAction(user.getUsername(), "LOGIN_FAILED", "Invalid password");
            throw AuthenticationException.invalidLogin();
        }
        if (!user.isActive()) {
            throw AuthenticationException.accountDisabled();
        }
        audit.recordAdminAction(user.getUsername(), "LOGIN", user.getRole() + " logged in");
        return new AuthResponse(jwt.issueToken(user), UserDto.from(user));
    }

    /** Self-registration is only offered to employees; staff accounts are created by administrators. */
    @Transactional
    public AuthResponse register(RegisterRequest r) {
        User user = userService.create(new CreateUserRequest(Role.EMPLOYEE, r.username(), r.password(),
                r.fullName(), r.email(), r.phone(), r.department(), null, null, null, null, null), r.username());
        return new AuthResponse(jwt.issueToken(user), UserDto.from(user));
    }
}
