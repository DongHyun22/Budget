package kr.co.budget.common;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class AppExceptionHandler {
    @ExceptionHandler(AppException.class)
    public String application(AppException exception, HttpServletResponse response, Model model) {
        return error(exception.getStatus().value(), exception.getMessage(), response, model);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String status(ResponseStatusException exception, HttpServletResponse response, Model model) {
        String message = exception.getStatusCode().value() == 404
                ? "요청한 항목을 찾을 수 없습니다." : "요청을 처리할 수 없습니다. 입력 내용을 확인해 주세요.";
        return error(exception.getStatusCode().value(), message, response, model);
    }

    @ExceptionHandler({ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class})
    public String invalidInput(Exception exception, HttpServletResponse response, Model model) {
        return error(400, "입력 형식을 확인해 주세요.", response, model);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String conflict(DataIntegrityViolationException exception, HttpServletResponse response, Model model) {
        return error(409, "이미 저장된 항목이거나 다른 기록에서 사용 중인 항목입니다.", response, model);
    }

    private String error(int status, String message, HttpServletResponse response, Model model) {
        response.setStatus(status);
        model.addAttribute("errorMessage", message);
        model.addAttribute("statusCode", status);
        return "error/message";
    }
}
