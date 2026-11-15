package com.itsm.service;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.user.Administrator;
import com.itsm.domain.user.Employee;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.TechnicianLevel;
import com.itsm.domain.user.User;
import com.itsm.repository.CategoryRepository;
import com.itsm.security.PasswordService;
import com.itsm.web.dto.UserDtos.CreateUserRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/** Factory pattern: callers ask for "a user with this role" and receive the right subclass. */
@Component
public class UserFactory {

    private final PasswordService passwords;
    private final CategoryRepository categories;

    public UserFactory(PasswordService passwords, CategoryRepository categories) {
        this.passwords = passwords;
        this.categories = categories;
    }

    public User create(CreateUserRequest r) {
        String hash = passwords.hash(r.password());
        String phone = r.phone() == null || r.phone().isBlank() ? null : r.phone().trim();
        String username = r.username().trim().toLowerCase();
        return switch (r.role()) {
            case EMPLOYEE -> new Employee(username, hash, r.fullName().trim(), r.email().trim(), phone,
                    r.department());
            case TECHNICIAN -> {
                Technician t = new Technician(username, hash, r.fullName().trim(), r.email().trim(), phone,
                        r.level() == null ? TechnicianLevel.L1 : r.level(),
                        r.experienceYears() == null ? 0 : r.experienceYears(),
                        r.maxWorkload() == null ? Technician.DEFAULT_MAX_WORKLOAD : r.maxWorkload());
                t.replaceSkills(resolveSkills(r.skillIds()));
                yield t;
            }
            case ADMIN -> new Administrator(username, hash, r.fullName().trim(), r.email().trim(), phone,
                    Boolean.TRUE.equals(r.itManager()));
        };
    }

    Set<Category> resolveSkills(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(categories.findAllById(ids));
    }
}
