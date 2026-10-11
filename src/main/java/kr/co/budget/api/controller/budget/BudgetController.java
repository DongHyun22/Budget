package kr.co.budget.api.controller.budget;

import kr.co.budget.api.service.budget.validation.BudgetInput;
import kr.co.budget.api.service.budget.BudgetService;
import kr.co.budget.domain.budget.Category;
import kr.co.budget.api.service.budget.CategoryService;
import kr.co.budget.domain.budget.Spend;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import kr.co.budget.api.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgets;
    private final CategoryService categories;
    private final MemberService members;
    private final Clock clock;

    @GetMapping("/budget")
    public String index(@RequestParam(required = false) String ym, Authentication authentication, Model model) {
        long memberId = members.requireMemberId(authentication);
        var today = LocalDate.now(clock);
        var month = ym == null ? YearMonth.from(today) : BudgetInput.month(ym);
        var summary = budgets.summary(memberId, month);
        model.addAttribute("summary", summary);
        model.addAttribute("categories", categories.list(memberId).stream().filter(Category::isActive).toList());
        model.addAttribute("today", today);
        model.addAttribute("defaultDate", YearMonth.from(today).equals(month) ? today : month.atDay(1));
        model.addAttribute("todayAllowance", summary.getDays().stream().filter(d -> d.getDate().equals(today)).findFirst().orElse(null));
        return "budget/index";
    }
    @PostMapping("/budget/plan")
    public String plan(@RequestParam String ym, @RequestParam Long amount, Authentication authentication) {
        var month = BudgetInput.month(ym);
        budgets.savePlan(members.requireMemberId(authentication), month, amount);
        return redirect(month);
    }
    @PostMapping("/budget/spends")
    public String add(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                      @RequestParam Long categoryId, @RequestParam Long amount,
                      @RequestParam(defaultValue = "") String memo, Authentication authentication) {
        budgets.addSpend(members.requireMemberId(authentication), date, categoryId, amount, memo);
        return redirect(YearMonth.from(date));
    }
    @GetMapping("/budget/spends/{id}")
    public String detail(@PathVariable long id, Authentication authentication, Model model) {
        long memberId = members.requireMemberId(authentication);
        var spend = budgets.detail(memberId, id);
        model.addAttribute("spend", spend);
        model.addAttribute("ym", YearMonth.from(spend.getDate()).toString());
        model.addAttribute("categories", categories.list(memberId).stream()
                .filter(c -> c.isActive() || c.getId().equals(spend.getCategoryId())).toList());
        return "budget/detail";
    }
    @PostMapping("/budget/spends/{id}")
    public String update(@PathVariable long id, @RequestParam Long categoryId, @RequestParam Long amount,
                         @RequestParam(defaultValue = "") String memo, Authentication authentication) {
        return redirect(budgets.updateSpend(members.requireMemberId(authentication), id, categoryId, amount, memo));
    }
    @PostMapping("/budget/spends/{id}/delete")
    public String delete(@PathVariable long id, Authentication authentication) {
        return redirect(budgets.deleteSpend(members.requireMemberId(authentication), id));
    }
    private String redirect(YearMonth month) { return "redirect:/budget?ym=" + month; }
}
