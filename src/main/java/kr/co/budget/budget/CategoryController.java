package kr.co.budget.budget;

import kr.co.budget.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller @RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categories;
    private final MemberService members;
    @GetMapping("/categories")
    public String list(Authentication authentication, Model model) {
        model.addAttribute("categories", categories.list(members.requireMemberId(authentication)));
        return "categories";
    }
    @PostMapping("/categories")
    public String add(@RequestParam String name, Authentication authentication) {
        categories.create(members.requireMemberId(authentication), name);
        return "redirect:/categories";
    }
    @PostMapping("/categories/{id}")
    public String update(@PathVariable long id, @RequestParam String name,
                         @RequestParam(defaultValue = "false") boolean active, Authentication authentication) {
        categories.update(members.requireMemberId(authentication), id, name, active);
        return "redirect:/categories";
    }
}
