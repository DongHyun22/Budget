package kr.co.budget.api.service.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import kr.co.budget.common.exception.AppException;
import kr.co.budget.domain.notification.Notification;
import kr.co.budget.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notifications;
    private final Clock clock;

    public Page<Notification> list(long memberId, int page) {
        if (page < 0 || page > 100000) throw new AppException(HttpStatus.BAD_REQUEST, "올바른 페이지를 입력하세요.");
        return notifications.findByMemberIdOrderByNoticeDateDesc(memberId, PageRequest.of(page, 20));
    }

    @Transactional
    public void markRead(long memberId, long id) {
        var notification = notifications.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
        notification.markRead(LocalDateTime.now(clock));
    }
}
