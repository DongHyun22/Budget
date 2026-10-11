package kr.co.budget.api.controller.notification;

import kr.co.budget.api.service.member.MemberService;
import kr.co.budget.api.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;
    private final MemberService members;

    @GetMapping("/notifications")
    public String list(@RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        model.addAttribute("notices", notifications.list(members.requireMemberId(authentication), page));
        return "notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String read(@PathVariable long id, Authentication authentication) {
        notifications.markRead(members.requireMemberId(authentication), id);
        return "redirect:/notifications";
    }
}
