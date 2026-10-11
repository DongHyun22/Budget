package kr.co.budget.notification;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import kr.co.budget.budget.BudgetService;
import kr.co.budget.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notices;
    private final MemberRepository members;
    private final BudgetService budgets;
    private final Clock clock;

    @Transactional
    public boolean generateForMember(long memberId, LocalDate date) {
        if (members.findByIdForUpdate(memberId).isEmpty()
                || notices.existsByMemberIdAndNoticeDate(memberId, date)) {
            return false;
        }
        var summary = budgets.summary(memberId, YearMonth.from(date));
        if (summary.getPlanAmount() == null) {
            return false;
        }
        var day = summary.getDays().get(date.getDayOfMonth() - 1);
        notices.saveAndFlush(new DailyNotification(memberId, date, day.getAllowed(), LocalDateTime.now(clock)));
        return true;
    }

    @Transactional(readOnly = true)
    public List<DailyNotification> list(long memberId) {
        return notices.findTop90ByMemberIdOrderByNoticeDateDesc(memberId);
    }

    @Transactional(readOnly = true)
    public Optional<DailyNotification> today(long memberId) {
        return notices.findByMemberIdAndNoticeDate(memberId, LocalDate.now(clock));
    }

    @Transactional
    public void markRead(long memberId, long id) {
        notices.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."))
                .markRead(LocalDateTime.now(clock));
    }
}
