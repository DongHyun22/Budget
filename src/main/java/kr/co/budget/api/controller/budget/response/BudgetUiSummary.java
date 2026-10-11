package kr.co.budget.api.controller.budget.response;

import kr.co.budget.api.service.budget.dto.BudgetSummary;
import kr.co.budget.domain.budget.Category;

import java.time.LocalDate;
import java.util.List;

public record BudgetUiSummary(String ym, String planAmount, String baseAmount, String totalSpent,
                              String remaining, String unallocated, List<Day> days, List<Entry> spends,
                              List<CategoryItem> categories, LocalDate today) {
    public record Day(LocalDate date, String base, String allowed, String spent, String remaining) { }
    public record Entry(String id, LocalDate date, String categoryId, String categoryName, String amount, String memo) { }
    public record CategoryItem(String id, String name, boolean active) {
        public static CategoryItem from(Category category) {
            return new CategoryItem(category.getId().toString(), category.getName(), category.isActive());
        }
    }

    public static BudgetUiSummary from(BudgetSummary summary, List<Category> categories, LocalDate today) {
        return new BudgetUiSummary(summary.getYm(), summary.getPlanAmount() == null ? null : summary.getPlanAmount().toString(),
                Long.toString(summary.getBaseAmount()), Long.toString(summary.getTotalSpent()),
                Long.toString(summary.getRemaining()), Long.toString(summary.getUnallocated()),
                summary.getDays().stream().map(day -> new Day(day.getDate(), Long.toString(day.getBase()),
                        Long.toString(day.getAllowed()), Long.toString(day.getSpent()), Long.toString(day.getRemaining()))).toList(),
                summary.getSpends().stream().map(entry -> new Entry(entry.getId().toString(), entry.getDate(),
                        entry.getCategoryId().toString(), entry.getCategoryName(), entry.getAmount().toString(), entry.getMemo())).toList(),
                categories.stream().map(CategoryItem::from).toList(), today);
    }
}
