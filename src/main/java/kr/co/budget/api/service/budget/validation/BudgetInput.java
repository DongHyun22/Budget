package kr.co.budget.api.service.budget.validation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class BudgetInput {
    public static final long MAX_AMOUNT = 99999999999999L;
    private BudgetInput() { }
    public static YearMonth month(String value) {
        try {
            if (value == null || !value.matches("[1-9][0-9]{3}-[0-9]{2}")) throw invalid("월은 YYYY-MM 형식으로 입력하세요.");
            return YearMonth.parse(value);
        } catch (DateTimeParseException ex) { throw invalid("올바른 월을 입력하세요."); }
    }
    public static void amount(Long amount) {
        if (amount == null || amount < 1 || amount > MAX_AMOUNT) throw invalid("금액은 1~99,999,999,999,999원으로 입력하세요.");
    }
    public static void date(LocalDate date) {
        if (date == null || date.getYear() < 1000 || date.getYear() > 9999) throw invalid("날짜는 1000~9999년 사이로 입력하세요.");
    }
    public static void memo(String memo) {
        if (memo != null && memo.length() > 200) throw invalid("메모는 200자 이하로 입력하세요.");
    }
    public static String name(String name) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 50) throw invalid("카테고리 이름은 1~50자로 입력하세요.");
        return name.trim();
    }
    public static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    public static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "항목을 찾을 수 없습니다."); }
}
