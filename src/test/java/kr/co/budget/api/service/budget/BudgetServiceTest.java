package kr.co.budget.api.service.budget;

import kr.co.budget.api.service.budget.validation.BudgetInput;
import kr.co.budget.domain.budget.repository.BudgetPlanRepository;
import kr.co.budget.domain.budget.Category;
import kr.co.budget.domain.budget.repository.CategoryRepository;
import kr.co.budget.domain.budget.Spend;
import kr.co.budget.domain.budget.repository.SpendRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import kr.co.budget.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class BudgetServiceTest {
    private final BudgetPlanRepository plans = mock(BudgetPlanRepository.class);
    private final SpendRepository spends = mock(SpendRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final CategoryService categoryService = new CategoryService(categories, members);
    private final BudgetService service = new BudgetService(plans, spends, categories, categoryService, members);
    private final LocalDate date = LocalDate.of(2026, 9, 1);

    @BeforeEach void defaults() { when(categories.findAllById(any())).thenReturn(List.of()); }
    @Test void scopesMonthlyQueryAndAllowsMissingPlan() {
        when(plans.findByMemberIdAndYm(7, "2026-09")).thenReturn(Optional.empty());
        when(spends.findAllByMemberIdAndSpendDateBetweenOrderBySpendDateAscIdAsc(7, date, date.withDayOfMonth(30)))
                .thenReturn(List.of(new Spend(7, date, 1, 200, "memo")));
        var summary = service.summary(7, YearMonth.of(2026, 9));
        assertThat(summary.getPlanAmount()).isNull();
        assertThat(summary.getTotalSpent()).isEqualTo(200);
        assertThat(summary.getDays()).hasSize(30);
    }
    @Test void foreignOrUnknownSpendIsNotFoundForAllOperations() {
        assertThatThrownBy(() -> service.detail(7, 100)).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.updateSpend(7, 100, 1L, 20L, "")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.deleteSpend(7, 100)).isInstanceOf(ResponseStatusException.class);
        verify(spends, never()).delete(any());
    }
    @Test void rejectsForeignAndInactiveCategories() {
        when(categories.findById(1L)).thenReturn(Optional.of(new Category(8L, "foreign")));
        assertThatThrownBy(() -> service.addSpend(7, date, 1L, 100L, "")).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        var inactive = new Category(7L, "inactive"); inactive.change("inactive", false);
        when(categories.findById(1L)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.addSpend(7, date, 1L, 100L, "")).isInstanceOf(ResponseStatusException.class);
        verify(spends, never()).save(any());
    }
    @Test void preservesDateAndExistingInactiveCategoryOnEdit() {
        var spend = new Spend(7, date, 1, 100, "before");
        var inactive = new Category(7L, "inactive"); inactive.change("inactive", false);
        when(spends.findByIdAndMemberId(10, 7)).thenReturn(Optional.of(spend));
        when(categories.findById(1L)).thenReturn(Optional.of(inactive));
        assertThat(service.updateSpend(7, 10, 1L, 500L, "after")).isEqualTo(YearMonth.of(2026, 9));
        assertThat(spend.getSpendDate()).isEqualTo(date);
        assertThat(spend.getAmount()).isEqualTo(500);
        assertThat(spend.getMemo()).isEqualTo("after");
    }
    @Test void rejectsOutOfRangeMoneyDateAndMemo() {
        for (Long invalid : List.of(0L, -1L, 100000000000000L)) {
            assertThatThrownBy(() -> service.addSpend(7, date, 1L, invalid, "")).isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> service.addSpend(7, date, 1L, 1L, "a".repeat(201))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.addSpend(7, LocalDate.of(999, 1, 1), 1L, 1L, "")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> BudgetInput.month("2026-13")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> BudgetInput.month("0999-01")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void acceptsFutureDateAndMaximumAmount() {
        when(categories.findById(1L)).thenReturn(Optional.of(new Category(null, "공용")));
        service.addSpend(7, LocalDate.of(9999, 12, 31), 1L, BudgetInput.MAX_AMOUNT, "");
        verify(spends).save(argThat(s -> s.getAmount() == BudgetInput.MAX_AMOUNT));
    }
}
