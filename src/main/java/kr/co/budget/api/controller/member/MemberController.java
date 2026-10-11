package kr.co.budget.api.controller.member;

import kr.co.budget.api.service.member.MemberService;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.budget.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService members;

    @GetMapping("/")
    public String home() {
        return "redirect:/budget";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String name, @RequestParam String email,
                         @RequestParam String password, @RequestParam String passwordConfirm,
                         Model model, HttpServletResponse response) {
        try {
            members.signup(name, email, password, passwordConfirm);
            return "redirect:/login?registered";
        } catch (AppException exception) {
            response.setStatus(exception.getStatus().value());
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "signup";
        }
    }
}
