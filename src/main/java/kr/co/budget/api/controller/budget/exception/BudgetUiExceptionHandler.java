package kr.co.budget.api.controller.budget.exception;

import kr.co.budget.api.controller.budget.BudgetUiController;

import kr.co.budget.common.exception.AppException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@Order(-1) @RestControllerAdvice(assignableTypes = BudgetUiController.class)
public class BudgetUiExceptionHandler {
    public record Error(String code, String message) { }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Error> status(ResponseStatusException exception) {
        return error(exception.getStatusCode().value());
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Error> application(AppException exception) {
        return error(exception.getStatus().value());
    }

    @ExceptionHandler({ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Error> input(Exception exception) { return error(400); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Error> conflict(Exception exception) { return error(409); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> unexpected(Exception exception) { return error(500); }

    private ResponseEntity<Error> error(int status) {
        Error body = switch (status) {
            case 400 -> new Error("INVALID_INPUT", "입력 형식과 허용 범위를 확인해 주세요.");
            case 401 -> new Error("UNAUTHENTICATED", "다시 로그인해 주세요.");
            case 403 -> new Error("FORBIDDEN", "페이지를 새로고침하거나 다시 로그인해 주세요.");
            case 404 -> new Error("NOT_FOUND", "요청한 항목을 찾을 수 없습니다.");
            case 409 -> new Error("CONFLICT", "이미 저장된 항목이거나 다른 기록에서 사용 중인 항목입니다.");
            default -> new Error("INTERNAL_ERROR", "요청을 처리하지 못했습니다. 잠시 후 다시 확인해 주세요.");
        };
        return ResponseEntity.status(status).body(body);
    }
}
