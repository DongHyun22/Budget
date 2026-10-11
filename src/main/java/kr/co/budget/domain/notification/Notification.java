package kr.co.budget.domain.notification;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_notification", uniqueConstraints = @UniqueConstraint(
        name = "uk_member_notice_date", columnNames = {"member_id", "notice_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_notification_seq")
    @SequenceGenerator(name = "member_notification_seq", sequenceName = "member_notification_seq", allocationSize = 1)
    private Long id;
    @Column(name = "member_id", nullable = false)
    private long memberId;
    @Column(name = "notice_date", nullable = false)
    private LocalDate noticeDate;
    @Column(name = "remaining_amount")
    private Long remainingAmount;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Notification(long memberId, LocalDate noticeDate, Long remainingAmount, LocalDateTime createdAt) {
        this.memberId = memberId;
        this.noticeDate = noticeDate;
        this.remainingAmount = remainingAmount;
        this.createdAt = createdAt;
    }

    public void markRead(LocalDateTime now) {
        if (readAt == null) readAt = now;
    }
}
