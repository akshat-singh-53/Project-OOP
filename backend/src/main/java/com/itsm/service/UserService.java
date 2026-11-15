package com.itsm.service;

import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Administrator;
import com.itsm.domain.user.Employee;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.UnauthorizedOperationException;
import com.itsm.exception.ValidationException;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.TechnicianRepository;
import com.itsm.repository.UserRepository;
import com.itsm.security.PasswordService;
import com.itsm.web.dto.UserDtos.CreateUserRequest;
import com.itsm.web.dto.UserDtos.UpdateUserRequest;
import com.itsm.web.dto.UserDtos.UserDto;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    public static final Set<IncidentStatus> ACTIVE_STATUSES = EnumSet.of(IncidentStatus.ASSIGNED,
            IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.PENDING_INFO);

    private final UserRepository users;
    private final TechnicianRepository technicians;
    private final IncidentRepository incidents;
    private final UserFactory factory;
    private final PasswordService passwords;
    private final AuditService audit;

    public UserService(UserRepository users, TechnicianRepository technicians, IncidentRepository incidents,
                       UserFactory factory, PasswordService passwords, AuditService audit) {
        this.users = users;
        this.technicians = technicians;
        this.incidents = incidents;
        this.factory = factory;
        this.passwords = passwords;
        this.audit = audit;
    }

    @Transactional
    public User create(CreateUserRequest request, String actor) {
        if (users.existsByUsernameIgnoreCase(request.username().trim())) {
            throw new ValidationException("Username '" + request.username() + "' is already taken");
        }
        User user = users.save(factory.create(request));
        audit.recordAdminAction(actor, "USER_CREATED", "Created " + user.getRole() + " " + user.getUsername());
        return user;
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public Technician getTechnician(Long id) {
        return technicians.findById(id).orElseThrow(() -> new ResourceNotFoundException("Technician", id));
    }

    @Transactional(readOnly = true)
    public List<UserDto> list(Role role) {
        Map<Long, Integer> workload = workloadByTechnician();
        return users.findAll(Sort.by("id")).stream()
                .filter(u -> role == null || u.getRole() == role)
                .map(u -> UserDto.from(u, u instanceof Technician ? workload.getOrDefault(u.getId(), 0) : null))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> workloadByTechnician() {
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : incidents.countWorkloadByTechnician(ACTIVE_STATUSES)) {
            map.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public int workloadOf(Technician technician) {
        return (int) incidents.countByAssignedTechnicianAndStatusIn(technician, ACTIVE_STATUSES);
    }

    @Transactional
    public User update(Long id, UpdateUserRequest r, User actor) {
        User user = get(id);
        user.updateProfile(r.fullName(), r.email(), r.phone());
        if (user instanceof Employee e && r.department() != null) {
            e.setDepartment(r.department());
        }
        if (user instanceof Technician t) {
            if (r.level() != null) t.setLevel(r.level());
            if (r.experienceYears() != null) t.setExperienceYears(r.experienceYears());
            if (r.maxWorkload() != null) t.setMaxWorkload(r.maxWorkload());
            if (r.available() != null) t.setAvailable(r.available());
            if (r.skillIds() != null) t.replaceSkills(factory.resolveSkills(r.skillIds()));
        }
        if (user instanceof Administrator a && r.itManager() != null) {
            a.setItManager(r.itManager());
        }
        if (r.active() != null) {
            if (!r.active() && user.getId().equals(actor.getId())) {
                throw new ValidationException("You cannot deactivate your own account");
            }
            if (r.active()) user.activate(); else user.deactivate();
        }
        audit.recordAdminAction(actor.getUsername(), "USER_UPDATED", "Updated user " + user.getUsername());
        return user;
    }

    @Transactional
    public void setAvailability(Technician technician, boolean available) {
        Technician t = getTechnician(technician.getId());
        t.setAvailable(available);
        audit.recordAdminAction(t.getUsername(), "AVAILABILITY_CHANGED", t.getFullName() + " is now "
                + (available ? "available" : "unavailable"));
    }

    @Transactional
    public void changePassword(User user, String current, String next) {
        User u = get(user.getId());
        if (!passwords.matches(current, u.getPasswordHash())) {
            throw new UnauthorizedOperationException("Current password is incorrect");
        }
        u.changePasswordHash(passwords.hash(next));
    }

    @Transactional
    public void resetPassword(Long id, String next, String actor) {
        User u = get(id);
        u.changePasswordHash(passwords.hash(next));
        audit.recordAdminAction(actor, "PASSWORD_RESET", "Password reset for " + u.getUsername());
    }

    public static boolean isOneOf(User user, Role... roles) {
        return Arrays.asList(roles).contains(user.getRole());
    }
}
