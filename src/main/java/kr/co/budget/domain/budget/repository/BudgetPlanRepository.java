package kr.co.budget.domain.budget.repository;

import kr.co.budget.domain.budget.BudgetPlan;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {
    Optional<BudgetPlan> findByMemberIdAndYm(long memberId, String ym);
}
