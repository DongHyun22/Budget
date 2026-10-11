package kr.co.budget.budget;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Collectors;
import kr.co.budget.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class BudgetService {
    private final BudgetPlanRepository plans;
    private final SpendRepository spends;
    private final CategoryRepository categories;
    private final CategoryService categoryService;
    private final MemberRepository members;

    public BudgetSummary summary(long memberId, YearMonth month) {
        BudgetInput.month(month.toString());
        Long amount = plans.findByMemberIdAndYm(memberId, month.toString()).map(BudgetPlan::getAmount).orElse(null);
        var entries = spends.findAllByMemberIdAndSpendDateBetweenOrderBySpendDateAscIdAsc(memberId, month.atDay(1), month.atEndOfMonth());
        var names = categories.findAllById(entries.stream().map(Spend::getCategoryId).distinct().toList()).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        var rows = entries.stream().map(s -> row(s, names.getOrDefault(s.getCategoryId(), "카테고리"))).toList();
        Map<LocalDate, Long> daily = entries.stream().collect(Collectors.toMap(Spend::getSpendDate, Spend::getAmount, Math::addExact));
        long total = entries.stream().mapToLong(Spend::getAmount).reduce(0L, Math::addExact);
        long base = amount == null ? 0 : amount / month.lengthOfMonth();
        return new BudgetSummary(month.toString(), amount, base, total, (amount == null ? 0 : amount) - total,
                amount == null ? 0 : amount % month.lengthOfMonth(), AllowanceCalculator.calculate(month, amount, daily), rows);
    }
    @Transactional public void savePlan(long memberId, YearMonth month, Long amount) {
        BudgetInput.month(month.toString()); BudgetInput.amount(amount);
        members.findByIdForUpdate(memberId).orElseThrow(BudgetInput::missing);
        var plan = plans.findByMemberIdAndYm(memberId, month.toString()).orElseGet(() -> new BudgetPlan(memberId, month.toString(), amount));
        plan.changeAmount(amount); plans.save(plan);
    }
    @Transactional public void addSpend(long memberId, LocalDate date, Long categoryId, Long amount, String memo) {
        BudgetInput.date(date); BudgetInput.amount(amount); BudgetInput.memo(memo);
        categoryService.usable(memberId, categoryId, null);
        spends.save(new Spend(memberId, date, categoryId, amount, memo));
    }
    public SpendRow detail(long memberId, long id) {
        var spend = owned(memberId, id);
        return row(spend, categories.findById(spend.getCategoryId()).map(Category::getName).orElse("카테고리"));
    }
    @Transactional public YearMonth updateSpend(long memberId, long id, Long categoryId, Long amount, String memo) {
        var spend = owned(memberId, id);
        BudgetInput.amount(amount); BudgetInput.memo(memo);
        categoryService.usable(memberId, categoryId, spend.getCategoryId());
        spend.change(categoryId, amount, memo);
        return YearMonth.from(spend.getSpendDate());
    }
    @Transactional public YearMonth deleteSpend(long memberId, long id) {
        var spend = owned(memberId, id); spends.delete(spend); return YearMonth.from(spend.getSpendDate());
    }
    private Spend owned(long memberId, long id) { return spends.findByIdAndMemberId(id, memberId).orElseThrow(BudgetInput::missing); }
    private SpendRow row(Spend spend, String name) {
        return new SpendRow(spend.getId(), spend.getSpendDate(), spend.getCategoryId(), name, spend.getAmount(), spend.getMemo());
    }
}
