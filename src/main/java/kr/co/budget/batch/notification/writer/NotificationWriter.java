package kr.co.budget.batch.notification.writer;

import kr.co.budget.domain.notification.Notification;
import kr.co.budget.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationWriter implements ItemWriter<Notification> {
    private final NotificationRepository notifications;

    @Override
    public void write(Chunk<? extends Notification> chunk) {
        for (var notification : chunk) {
            if (!notifications.existsByMemberIdAndNoticeDate(notification.getMemberId(), notification.getNoticeDate())) {
                notifications.save(notification);
            }
        }
        notifications.flush();
    }
}
