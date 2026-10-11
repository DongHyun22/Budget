package kr.co.budget.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import kr.co.budget.budget.BudgetService;
import kr.co.budget.member.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationDashboardTest {
    @Autowired MockMvc mvc;
    @Autowired MemberService members;
    @Autowired BudgetService budgets;
    @Autowired NotificationService notifications;
    @Autowired Clock clock;

    @Test
    void includesTodaysGeneratedNotificationOnFirstScreen() throws Exception {
        String email = "dashboard@example.test";
        long id = members.signup("첫 화면", email, "Password123!", "Password123!").getId();
        LocalDate today = LocalDate.now(clock);
        budgets.savePlan(id, YearMonth.from(today), 300000L);
        notifications.generateForMember(id, today);
        mvc.perform(get("/budget").with(user(email)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("dailyNotice", notifications.list(id).get(0)));
    }
}
