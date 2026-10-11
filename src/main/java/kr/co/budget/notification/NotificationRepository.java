package kr.co.budget.notification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<DailyNotification, Long> {
    boolean existsByMemberIdAndNoticeDate(long memberId, LocalDate date);
    Optional<DailyNotification> findByMemberIdAndNoticeDate(long memberId, LocalDate date);
    List<DailyNotification> findTop90ByMemberIdOrderByNoticeDateDesc(long memberId);
    Optional<DailyNotification> findByIdAndMemberId(long id, long memberId);
}
