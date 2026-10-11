package kr.co.budget.api.controller.budget;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import kr.co.budget.api.service.member.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class BudgetMvcBaselineTest {
    @Autowired MockMvc mvc;
    @Autowired MemberService members;

    @Test void dashboardWorksWithoutNotificationsAndRemovedRoutesAreUnavailable() throws Exception {
        String email = "dashboard-without-notices@example.test";
        members.signup("가계부", email, "Password123!", "Password123!");
        mvc.perform(get("/budget").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(view().name("budget/index"))
                .andExpect(model().attributeExists("summary", "categories", "today"))
                .andExpect(model().attributeDoesNotExist("dailyNotice"));
        mvc.perform(get("/notifications").with(user(email)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/notifications/1/read").with(user(email)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test void mvcRedirectAndOwnershipContractRemainIntact() throws Exception {
        members.signup("MVC", "mvc-baseline@example.test", "Password123!", "Password123!");
        mvc.perform(get("/budget")).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(post("/budget/plan").with(user("mvc-baseline@example.test")).with(csrf())
                .param("ym", "2027-01").param("amount", "310000"))
                .andExpect(redirectedUrl("/budget?ym=2027-01"));
        mvc.perform(get("/budget/spends/999999999").with(user("mvc-baseline@example.test")))
                .andExpect(status().isNotFound()).andExpect(view().name("error/message"));
    }
}
