package com.itsm.web;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.security.CurrentUser;
import com.itsm.security.RequiresRole;
import com.itsm.service.NotificationService;
import com.itsm.web.dto.IncidentDtos.NotificationDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationDto> inbox(@CurrentUser User user, @RequestParam(defaultValue = "30") int limit) {
        return notifications.inbox(user, Math.min(limit, 200)).stream().map(NotificationDto::from).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@CurrentUser User user) {
        return Map.of("count", notifications.unreadCount(user));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, @CurrentUser User user) {
        notifications.markRead(user, id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@CurrentUser User user) {
        notifications.markAllRead(user);
    }

    /** Every message sent over every channel, including simulated e-mail and SMS. */
    @GetMapping("/outbox")
    @RequiresRole(Role.ADMIN)
    public List<NotificationDto> outbox(@RequestParam(defaultValue = "200") int limit) {
        return notifications.outbox(Math.min(limit, 1000)).stream().map(NotificationDto::from).toList();
    }
}
