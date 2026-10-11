package kr.co.budget.api.service.budget.dto;

import java.util.List;
import lombok.Value;

@Value
public class BudgetSummary {
    String ym;
    Long planAmount;
    long baseAmount;
    long totalSpent;
    long remaining;
    long unallocated;
    List<DayAllowance> days;
    List<SpendRow> spends;
}
