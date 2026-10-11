package kr.co.budget.api.controller.budget;

import kr.co.budget.api.service.budget.validation.BudgetInput;
import kr.co.budget.api.service.budget.BudgetService;
import kr.co.budget.api.controller.budget.response.BudgetUiSummary;
import kr.co.budget.domain.budget.Category;
import kr.co.budget.api.service.budget.CategoryService;

import java.time.Clock;
import java.time.LocalDate;
import kr.co.budget.api.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/budget/ui") @RequiredArgsConstructor
public class BudgetUiController {
    private final BudgetService budgets;
    private final CategoryService categories;
    private final MemberService members;
    private final Clock clock;

    @GetMapping("/summary")
    public BudgetUiSummary summary(@RequestParam String ym, Authentication authentication) {
        long memberId = members.requireMemberId(authentication);
        return BudgetUiSummary.from(budgets.summary(memberId, BudgetInput.month(ym)), categories.list(memberId), LocalDate.now(clock));
    }

    @PostMapping("/plan") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void plan(@RequestParam String ym, @RequestParam Long amount, Authentication authentication) {
        budgets.savePlan(members.requireMemberId(authentication), BudgetInput.month(ym), amount);
    }

    @PostMapping("/spends") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                    @RequestParam Long categoryId, @RequestParam Long amount,
                    @RequestParam(defaultValue = "") String memo, Authentication authentication) {
        budgets.addSpend(members.requireMemberId(authentication), date, categoryId, amount, memo);
    }

    @PostMapping("/spends/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable long id, @RequestParam Long categoryId, @RequestParam Long amount,
                       @RequestParam(defaultValue = "") String memo, Authentication authentication) {
        budgets.updateSpend(members.requireMemberId(authentication), id, categoryId, amount, memo);
    }

    @PostMapping("/spends/{id}/delete") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication authentication) {
        budgets.deleteSpend(members.requireMemberId(authentication), id);
    }

    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    public BudgetUiSummary.CategoryItem category(@RequestParam String name, Authentication authentication) {
        return BudgetUiSummary.CategoryItem.from(categories.create(members.requireMemberId(authentication), name));
    }
}
