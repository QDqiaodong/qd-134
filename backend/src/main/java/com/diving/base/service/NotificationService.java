package com.diving.base.service;

import com.diving.base.dto.response.PageResponse;
import com.diving.base.entity.Equipment;
import com.diving.base.entity.Notification;
import com.diving.base.entity.Team;
import com.diving.base.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 深度下调导致装备解绑后，向小组推送站内通知。
     * 未订阅深度变动的小组不推送；没有解绑装备时不推送。
     */
    @Transactional
    public Notification notifyDepthUnbind(Team team, Integer newMaxDepth, List<Equipment> removedEquipments) {
        if (removedEquipments == null || removedEquipments.isEmpty()) {
            return null;
        }

        if (team.getDepthChangeNotify() == null || !team.getDepthChangeNotify()) {
            log.info("小组{}未订阅深度变动通知，跳过推送", team.getId());
            return null;
        }

        String equipmentNames = removedEquipments.stream()
                .map(Equipment::getName)
                .collect(Collectors.joining("、"));

        String title = "深度下调解绑通知";
        String content = String.format(
                "小组「%s」最大允许深度已下调至%dm，共解绑%d件超出新深度的装备：%s。",
                team.getName(), newMaxDepth, removedEquipments.size(), equipmentNames);

        Notification notification = Notification.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .title(title)
                .content(content)
                .notifyType("DEPTH_CHANGE")
                .removedCount(removedEquipments.size())
                .equipmentNames(equipmentNames)
                .readStatus(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("已向小组{}推送深度下调解绑通知，解绑{}件装备", team.getId(), removedEquipments.size());
        return saved;
    }

    /**
     * 通知列表不按已读状态缓存，始终读数据库，保证刷新后阅读状态一致。
     */
    public PageResponse<Notification> findAll(int page, int size, Long teamId, Boolean readStatus) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage;

        if (teamId != null && readStatus != null) {
            notificationPage = notificationRepository.findByTeamIdAndReadStatus(teamId, readStatus, pageable);
        } else if (teamId != null) {
            notificationPage = notificationRepository.findByTeamId(teamId, pageable);
        } else if (readStatus != null) {
            notificationPage = notificationRepository.findByReadStatus(readStatus, pageable);
        } else {
            notificationPage = notificationRepository.findAll(pageable);
        }

        return PageResponse.from(notificationPage);
    }

    public Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("通知不存在: " + id));
    }

    /**
     * 成员打开通知后标记为已读，幂等：已读的通知直接返回。
     */
    @Transactional
    public Notification markAsRead(Long id) {
        Notification notification = findById(id);
        if (Boolean.TRUE.equals(notification.getReadStatus())) {
            return notification;
        }
        notification.setReadStatus(true);
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public long countUnread(Long teamId) {
        if (teamId != null) {
            return notificationRepository.countByTeamIdAndReadStatus(teamId, false);
        }
        return notificationRepository.countByReadStatus(false);
    }
}
