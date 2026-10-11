package kr.co.budget.budget;

import java.time.LocalDate;
import lombok.Value;

@Value
public class DayAllowance {
    LocalDate date;
    long base;
    long allowed;
    long spent;
    long remaining;
}
