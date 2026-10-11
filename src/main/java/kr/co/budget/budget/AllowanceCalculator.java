package kr.co.budget.budget;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AllowanceCalculator {
    private AllowanceCalculator() { }

    public static List<DayAllowance> calculate(YearMonth month, Long amount, Map<LocalDate, Long> spends) {
        long base = amount == null ? 0 : amount / month.lengthOfMonth();
        long carry = 0;
        var days = new ArrayList<DayAllowance>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            var date = month.atDay(day);
            long spent = spends.getOrDefault(date, 0L);
            long allowed = Math.addExact(base, carry);
            carry = Math.subtractExact(allowed, spent);
            days.add(new DayAllowance(date, base, allowed, spent, carry));
        }
        return List.copyOf(days);
    }
}
