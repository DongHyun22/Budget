package kr.co.budget.notification;

import kr.co.budget.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class NotificationController {
    private final MemberService members;
    private final NotificationService notifications;

    @GetMapping("/notifications")
    public String list(Authentication authentication, Model model) {
        model.addAttribute("notifications", notifications.list(members.requireMemberId(authentication)));
        return "notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String read(Authentication authentication, @PathVariable long id) {
        notifications.markRead(members.requireMemberId(authentication), id);
        return "redirect:/notifications";
    }
}
