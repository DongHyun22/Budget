package kr.co.budget.api.controller.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.co.budget.api.service.member.MemberService;
import kr.co.budget.domain.member.repository.MemberRepository;
import kr.co.budget.domain.notification.Notification;
import kr.co.budget.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired MemberService memberService;
    @Autowired MemberRepository members;
    @Autowired NotificationRepository notifications;

    @Test
    void listsOnlyOwnNoticesAndReadRequiresOwnershipAndCsrf() throws Exception {
        String owner = "notice-owner@example.test";
        String other = "notice-other@example.test";
        memberService.signup("owner", owner, "Password123!", "Password123!");
        memberService.signup("other", other, "Password123!", "Password123!");
        long ownerId = members.findByEmail(owner).orElseThrow().getId();
        long otherId = members.findByEmail(other).orElseThrow().getId();
        var notice = notifications.saveAndFlush(new Notification(ownerId, LocalDate.now(), 0L, LocalDateTime.now()));
        notifications.saveAndFlush(new Notification(otherId, LocalDate.now(), null, LocalDateTime.now()));
        mvc.perform(get("/notifications")).andExpect(redirectedUrlPattern("**/login"));
        var response = mvc.perform(get("/notifications").with(user(owner)))
                .andExpect(status().isOk()).andExpect(view().name("notifications")).andReturn();
        var page = (Page<?>) response.getModelAndView().getModel().get("notices");
        assertThat(page.getContent()).hasSize(1);
        assertThat(((Notification) page.getContent().get(0)).getMemberId()).isEqualTo(ownerId);
        mvc.perform(post("/notifications/{id}/read", notice.getId()).with(user(owner)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/notifications/{id}/read", notice.getId()).with(user(other)).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(notice.getReadAt()).isNull();
        mvc.perform(post("/notifications/{id}/read", notice.getId()).with(user(owner)).with(csrf()))
                .andExpect(redirectedUrl("/notifications"));
        var readAt = notice.getReadAt();
        assertThat(readAt).isNotNull();
        mvc.perform(post("/notifications/{id}/read", notice.getId()).with(user(owner)).with(csrf()))
                .andExpect(redirectedUrl("/notifications"));
        assertThat(notice.getReadAt()).isEqualTo(readAt);
        mvc.perform(get("/notifications?page=-1").with(user(owner))).andExpect(status().isBadRequest());
    }
}
