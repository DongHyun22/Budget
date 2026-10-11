package kr.co.budget.batch.notification.processor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import kr.co.budget.api.service.budget.BudgetService;
import kr.co.budget.domain.notification.Notification;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class NotificationProcessor implements ItemProcessor<Long, Notification> {
    private final BudgetService budgets;
    private final Clock clock;
    private final LocalDate noticeDate;

    public NotificationProcessor(BudgetService budgets, Clock clock,
            @Value("#{jobParameters['noticeDate']}") String noticeDate) {
        this.budgets = budgets;
        this.clock = clock;
        this.noticeDate = LocalDate.parse(noticeDate);
    }

    @Override
    public Notification process(Long memberId) {
        var summary = budgets.summary(memberId, YearMonth.from(noticeDate));
        Long remaining = summary.getPlanAmount() == null ? null
                : summary.getDays().get(noticeDate.getDayOfMonth() - 1).getRemaining();
        return new Notification(memberId, noticeDate, remaining, LocalDateTime.now(clock));
    }
}
