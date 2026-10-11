package kr.co.budget.domain.notification.repository;

import java.time.LocalDate;
import java.util.Optional;
import kr.co.budget.domain.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByMemberIdAndNoticeDate(long memberId, LocalDate noticeDate);
    Page<Notification> findByMemberIdOrderByNoticeDateDesc(long memberId, Pageable pageable);
    Optional<Notification> findByIdAndMemberId(long id, long memberId);
}
