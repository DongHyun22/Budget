package kr.co.budget.budget;

import java.time.LocalDate;
import lombok.Value;

@Value
public class SpendRow {
    Long id;
    LocalDate date;
    Long categoryId;
    String categoryName;
    Long amount;
    String memo;
}
