package com.diving.base.repository;

import com.diving.base.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByTeamId(Long teamId, Pageable pageable);

    Page<Notification> findByReadStatus(Boolean readStatus, Pageable pageable);

    Page<Notification> findByTeamIdAndReadStatus(Long teamId, Boolean readStatus, Pageable pageable);

    long countByTeamIdAndReadStatus(Long teamId, Boolean readStatus);

    long countByReadStatus(Boolean readStatus);
}
