package kr.co.budget.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.budget.member.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class BudgetUiTest {
    @Autowired MockMvc mvc;
    @Autowired MemberService members;
    @Autowired CategoryRepository categories;
    @Autowired SpendRepository spends;
    @Autowired ObjectMapper json;
    long memberId;
    String categoryId;
    static final String EMAIL = "ui-test@example.test";

    @BeforeEach void fixtures() {
        memberId = members.signup("UI", EMAIL, "Password123!", "Password123!").getId();
        categoryId = categories.save(new Category(memberId, "Food")).getId().toString();
    }

    @Test void summaryAndCrudUseStringsAndDoNotExposeEntities() throws Exception {
        mvc.perform(get("/budget/ui/summary").with(user(EMAIL)).param("ym", "2027-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.planAmount").doesNotExist());
        mvc.perform(post("/budget/ui/plan").with(user(EMAIL)).with(csrf())
                .param("ym", "2027-01").param("amount", "310000")).andExpect(status().isNoContent());
        mvc.perform(post("/budget/ui/spends").with(user(EMAIL)).with(csrf())
                .param("date", "2027-01-02").param("categoryId", categoryId)
                .param("amount", "12000").param("memo", "lunch")).andExpect(status().isNoContent());
        String body = mvc.perform(get("/budget/ui/summary").with(user(EMAIL)).param("ym", "2027-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.planAmount").value("310000"))
                .andExpect(jsonPath("$.totalSpent").value("12000"))
                .andExpect(jsonPath("$.remaining").value("298000"))
                .andExpect(jsonPath("$.days[1].spent").value("12000"))
                .andExpect(jsonPath("$.spends[0].id").isString())
                .andExpect(jsonPath("$.spends[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$.categories[0].active").value(true))
                .andExpect(jsonPath("$.today").isString()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("memberId", "password", "normalizedName");
        String id = json.readTree(body).path("spends").get(0).path("id").asText();
        mvc.perform(post("/budget/ui/spends/" + id).with(user(EMAIL)).with(csrf())
                .param("categoryId", categoryId).param("amount", "15000")).andExpect(status().isNoContent());
        mvc.perform(post("/budget/ui/spends/" + id + "/delete").with(user(EMAIL)).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/budget/ui/summary").with(user(EMAIL)).param("ym", "2027-01"))
                .andExpect(jsonPath("$.totalSpent").value("0")).andExpect(jsonPath("$.spends").isEmpty());
    }

    @Test void securityFailuresAreJsonAndMvcStillRedirects() throws Exception {
        mvc.perform(get("/budget/ui/summary").param("ym", "2027-01"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(post("/budget/ui/plan").with(user(EMAIL)).param("ym", "2027-01").param("amount", "1"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/budget")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test void malformedInputAndMoneyBoundsReturnJson() throws Exception {
        mvc.perform(get("/budget/ui/summary").with(user(EMAIL)).param("ym", "2027-13"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        for (String value : new String[]{"0", "100000000000000", "9223372036854775808", "abc"}) {
            mvc.perform(post("/budget/ui/plan").with(user(EMAIL)).with(csrf()).param("ym", "2027-01").param("amount", value))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
        mvc.perform(post("/budget/ui/spends").with(user(EMAIL)).with(csrf()).param("date", "bad"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(post("/budget/ui/plan").with(user(EMAIL)).with(csrf()).param("ym", "2027-01").param("amount", "99999999999999"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/budget/ui/summary").with(user(EMAIL)).param("ym", "2027-01"))
                .andExpect(jsonPath("$.planAmount").value("99999999999999"));
    }

    @Test void categoryNormalizationAndInactiveRulesArePreserved() throws Exception {
        mvc.perform(post("/budget/ui/categories").with(user(EMAIL)).with(csrf()).param("name", "  Travel  "))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("Travel")).andExpect(jsonPath("$.active").value(true));
        mvc.perform(post("/budget/ui/categories").with(user(EMAIL)).with(csrf()).param("name", " travel "))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
        for (String name : new String[]{" ", "a".repeat(51)}) {
            mvc.perform(post("/budget/ui/categories").with(user(EMAIL)).with(csrf()).param("name", name))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }
        categories.findById(Long.valueOf(categoryId)).orElseThrow().change("Food", false);
        var spend = spends.save(new Spend(memberId, LocalDate.of(2027, 1, 2), Long.parseLong(categoryId), 100, ""));
        mvc.perform(post("/budget/ui/spends").with(user(EMAIL)).with(csrf()).param("date", "2027-01-02")
                .param("categoryId", categoryId).param("amount", "100")).andExpect(status().isBadRequest());
        mvc.perform(post("/budget/ui/spends/" + spend.getId()).with(user(EMAIL)).with(csrf())
                .param("categoryId", categoryId).param("amount", "200")).andExpect(status().isNoContent());
        mvc.perform(get("/budget/ui/summary").with(user(EMAIL)).param("ym", "2027-01"))
                .andExpect(jsonPath("$.categories[0].active").value(false));
    }

    @Test void foreignOwnershipReturns404AndDoesNotLeakData() throws Exception {
        long other = members.signup("Other", "ui-other@example.test", "Password123!", "Password123!").getId();
        var foreign = categories.save(new Category(other, "Private category"));
        var spend = spends.save(new Spend(other, LocalDate.of(2027, 1, 2), foreign.getId(), 100, "Private memo"));
        mvc.perform(post("/budget/ui/spends/" + spend.getId()).with(user(EMAIL)).with(csrf())
                .param("categoryId", categoryId).param("amount", "200"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(post("/budget/ui/spends/" + spend.getId() + "/delete").with(user(EMAIL)).with(csrf()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(post("/budget/ui/spends").with(user(EMAIL)).with(csrf()).param("date", "2027-01-02")
                .param("categoryId", foreign.getId().toString()).param("amount", "100"))
                .andExpect(status().isNotFound());
        String body = mvc.perform(get("/budget/ui/summary").with(user(EMAIL)).param("ym", "2027-01"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Private");
        assertThat(spends.findById(spend.getId())).isPresent();
    }
}
