package kr.co.budget.budget;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AllowanceCalculatorTest {
    @Test void preservesOriginalCarryIncludingOverspend() {
        var days = AllowanceCalculator.calculate(YearMonth.of(2026, 9), 300000L,
                Map.of(LocalDate.of(2026, 9, 1), 12000L, LocalDate.of(2026, 9, 2), 3000L));
        assertThat(days.get(0).getAllowed()).isEqualTo(10000);
        assertThat(days.get(0).getRemaining()).isEqualTo(-2000);
        assertThat(days.get(1).getAllowed()).isEqualTo(8000);
        assertThat(days.get(1).getRemaining()).isEqualTo(5000);
        assertThat(days.get(2).getAllowed()).isEqualTo(15000);
    }
    @Test void resetsAtMonthBoundaryAndDoesNotDistributeRemainder() {
        var days = AllowanceCalculator.calculate(YearMonth.of(2026, 10), 100L, Map.of());
        assertThat(days.get(0).getAllowed()).isEqualTo(3);
        assertThat(days.get(30).getRemaining()).isEqualTo(93);
    }
    @Test void handlesLeapYearAndMissingPlan() {
        var days = AllowanceCalculator.calculate(YearMonth.of(2024, 2), null,
                Map.of(LocalDate.of(2024, 2, 29), 500L));
        assertThat(days).hasSize(29);
        assertThat(days.get(27).getRemaining()).isZero();
        assertThat(days.get(28).getRemaining()).isEqualTo(-500);
    }
}
