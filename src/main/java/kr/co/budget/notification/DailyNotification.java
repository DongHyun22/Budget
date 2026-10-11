package kr.co.budget.notification;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_notification", uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "notice_date"}))
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class DailyNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_seq")
    @SequenceGenerator(name = "notification_seq", sequenceName = "notification_seq", allocationSize = 1)
    private Long id;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(name = "notice_date", nullable = false)
    private LocalDate noticeDate;
    @Column(name = "opening_amount", nullable = false)
    private Long openingAmount;
    private LocalDateTime readAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public DailyNotification(long memberId, LocalDate date, long amount, LocalDateTime now) {
        this.memberId = memberId;
        this.noticeDate = date;
        this.openingAmount = amount;
        this.createdAt = now;
    }

    public void markRead(LocalDateTime now) {
        if (readAt == null) {
            readAt = now;
        }
    }
}
