package com.itsm.web;

import com.itsm.domain.user.User;
import com.itsm.security.CurrentUser;
import com.itsm.service.AuthService;
import com.itsm.service.UserService;
import com.itsm.web.dto.UserDtos.AuthResponse;
import com.itsm.web.dto.UserDtos.ChangePasswordRequest;
import com.itsm.web.dto.UserDtos.LoginRequest;
import com.itsm.web.dto.UserDtos.RegisterRequest;
import com.itsm.web.dto.UserDtos.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public UserDto me(@CurrentUser User user) {
        return UserDto.from(user);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUser User user, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user, request.currentPassword(), request.newPassword());
    }
}
