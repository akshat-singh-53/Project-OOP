package com.itsm.web.dto;

import com.itsm.domain.user.Administrator;
import com.itsm.domain.user.Employee;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.TechnicianLevel;
import com.itsm.domain.user.User;
import com.itsm.web.dto.CatalogDtos.CategoryRef;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserDto(Long id, String userCode, String username, String fullName, String email, String phone,
                          Role role, boolean active, LocalDateTime createdAt,
                          String department,
                          TechnicianLevel level, String levelTitle, Integer experienceYears, Integer maxWorkload,
                          Boolean available, List<CategoryRef> skills, Integer activeWorkload,
                          Boolean itManager) {

        public static UserDto from(User u) {
            return from(u, null);
        }

        /** Uses pattern matching on the concrete subtype to expose role-specific fields. */
        public static UserDto from(User u, Integer workload) {
            String department = null;
            TechnicianLevel level = null;
            Integer exp = null;
            Integer max = null;
            Boolean available = null;
            List<CategoryRef> skills = null;
            Boolean itManager = null;
            if (u instanceof Employee e) {
                department = e.getDepartment();
            } else if (u instanceof Technician t) {
                level = t.getLevel();
                exp = t.getExperienceYears();
                max = t.getMaxWorkload();
                available = t.isAvailable();
                skills = t.getSkills().stream().map(CategoryRef::from)
                        .sorted(Comparator.comparing(CategoryRef::name)).toList();
            } else if (u instanceof Administrator a) {
                itManager = a.isItManager();
            }
            return new UserDto(u.getId(), u.getUserCode(), u.getUsername(), u.getFullName(), u.getEmail(),
                    u.getPhone(), u.getRole(), u.isActive(), u.getCreatedAt(), department, level,
                    level == null ? null : level.getTitle(), exp, max, available, skills, workload, itManager);
        }
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+",
                    message = "only letters, digits, dot, underscore and dash") String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email String email,
            @Pattern(regexp = "^$|^[0-9+ -]{7,20}$", message = "invalid phone number") String phone,
            @Size(max = 80) String department) {
    }

    public record AuthResponse(String token, UserDto user) {
    }

    public record CreateUserRequest(
            @NotNull Role role,
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+",
                    message = "only letters, digits, dot, underscore and dash") String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email String email,
            @Pattern(regexp = "^$|^[0-9+ -]{7,20}$", message = "invalid phone number") String phone,
            String department,
            TechnicianLevel level,
            @Min(0) @Max(50) Integer experienceYears,
            @Min(1) @Max(50) Integer maxWorkload,
            Set<Long> skillIds,
            Boolean itManager) {
    }

    public record UpdateUserRequest(
            @Size(max = 100) String fullName,
            @Email String email,
            @Pattern(regexp = "^$|^[0-9+ -]{7,20}$", message = "invalid phone number") String phone,
            String department,
            TechnicianLevel level,
            @Min(0) @Max(50) Integer experienceYears,
            @Min(1) @Max(50) Integer maxWorkload,
            Boolean available,
            Set<Long> skillIds,
            Boolean itManager,
            Boolean active) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 6) String newPassword) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 6) String newPassword) {
    }

    public record AvailabilityRequest(@NotNull Boolean available) {
    }
}
