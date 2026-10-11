package kr.co.budget.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.LocalDate;
import java.time.YearMonth;
import kr.co.budget.budget.BudgetService;
import kr.co.budget.budget.Category;
import kr.co.budget.budget.CategoryRepository;
import kr.co.budget.member.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {
    @Autowired NotificationService notifications;
    @Autowired MemberService members;
    @Autowired BudgetService budgets;
    @Autowired CategoryRepository categories;

    @Test
    void preservesStartOfDaySnapshotAndIgnoresTodaySpending() {
        long id = members.signup("알림 검증", "notice1@example.test", "Password123!", "Password123!").getId();
        var category = categories.saveAndFlush(new Category(id, "식비"));
        budgets.savePlan(id, YearMonth.of(2026, 9), 300000L);
        budgets.addSpend(id, LocalDate.of(2026, 9, 1), category.getId(), 12000L, "전날");
        budgets.addSpend(id, LocalDate.of(2026, 9, 2), category.getId(), 3000L, "오늘");
        LocalDate date = LocalDate.of(2026, 9, 2);
        assertThat(notifications.generateForMember(id, date)).isTrue();
        assertThat(notifications.generateForMember(id, date)).isFalse();
        assertThat(notifications.list(id)).hasSize(1);
        var notice = notifications.list(id).get(0);
        assertThat(notice.getOpeningAmount()).isEqualTo(8000L);
        budgets.savePlan(id, YearMonth.of(2026, 9), 600000L);
        assertThat(notifications.generateForMember(id, date)).isFalse();
        assertThat(notifications.list(id).get(0).getOpeningAmount()).isEqualTo(8000L);
        notifications.markRead(id, notice.getId());
        var firstRead = notice.getReadAt();
        notifications.markRead(id, notice.getId());
        assertThat(notice.getReadAt()).isEqualTo(firstRead).isNotNull();
    }

    @Test
    void skipsMissingPlanAndIsolatesMembers() {
        long first = members.signup("회원 가", "notice2@example.test", "Password123!", "Password123!").getId();
        long second = members.signup("회원 나", "notice3@example.test", "Password123!", "Password123!").getId();
        LocalDate date = LocalDate.of(2026, 10, 1);
        assertThat(notifications.generateForMember(first, date)).isFalse();
        budgets.savePlan(first, YearMonth.from(date), 310000L);
        assertThat(notifications.generateForMember(first, date)).isTrue();
        var notice = notifications.list(first).get(0);
        assertThat(notice.getOpeningAmount()).isEqualTo(10000L);
        assertThat(notifications.list(second)).isEmpty();
        assertThatThrownBy(() -> notifications.markRead(second, notice.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(404));
    }
}
