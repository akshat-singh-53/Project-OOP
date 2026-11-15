package com.itsm.web;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.exception.UnauthorizedOperationException;
import com.itsm.security.CurrentUser;
import com.itsm.security.RequiresRole;
import com.itsm.service.UserService;
import com.itsm.web.dto.UserDtos.AvailabilityRequest;
import com.itsm.web.dto.UserDtos.CreateUserRequest;
import com.itsm.web.dto.UserDtos.ResetPasswordRequest;
import com.itsm.web.dto.UserDtos.UpdateUserRequest;
import com.itsm.web.dto.UserDtos.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/users")
    @RequiresRole(Role.ADMIN)
    public List<UserDto> list(@RequestParam(required = false) Role role) {
        return users.list(role);
    }

    @PostMapping("/users")
    @RequiresRole(Role.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest r, @CurrentUser User actor) {
        return UserDto.from(users.create(r, actor.getUsername()));
    }

    @PutMapping("/users/{id}")
    @RequiresRole(Role.ADMIN)
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest r, @CurrentUser User actor) {
        return UserDto.from(users.update(id, r, actor));
    }

    @PostMapping("/users/{id}/reset-password")
    @RequiresRole(Role.ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest r, @CurrentUser User actor) {
        users.resetPassword(id, r.newPassword(), actor.getUsername());
    }

    /** Support staff need the technician list (with live workload) for assignment decisions. */
    @GetMapping("/technicians")
    @RequiresRole({Role.ADMIN, Role.TECHNICIAN})
    public List<UserDto> technicians() {
        return users.list(Role.TECHNICIAN).stream().filter(UserDto::active).toList();
    }

    @PutMapping("/technicians/me/availability")
    @RequiresRole(Role.TECHNICIAN)
    public UserDto setAvailability(@Valid @RequestBody AvailabilityRequest r, @CurrentUser User actor) {
        if (!(actor instanceof Technician technician)) {
            throw new UnauthorizedOperationException("Only technicians have availability");
        }
        users.setAvailability(technician, r.available());
        return UserDto.from(users.get(actor.getId()), users.workloadOf(users.getTechnician(actor.getId())));
    }
}
