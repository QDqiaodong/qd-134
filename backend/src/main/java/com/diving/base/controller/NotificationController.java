package com.diving.base.controller;

import com.diving.base.dto.response.ApiResponse;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.entity.Notification;
import com.diving.base.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Notification>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Boolean readStatus) {
        PageResponse<Notification> result = notificationService.findAll(page, size, teamId, readStatus);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Notification>> getById(@PathVariable Long id) {
        Notification notification = notificationService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Notification>> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("已标记为已读", notification));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.countUnread(teamId)));
    }
}
