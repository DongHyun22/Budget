package kr.co.budget.config;

import kr.co.budget.domain.member.Member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberSecurityTest {
    @Autowired MockMvc mvc;

    @Test
    void authenticatedRootRedirectsToBudget() throws Exception {
        mvc.perform(get("/").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("member@example.test")))
                .andExpect(redirectedUrl("/budget"));
    }

    @Test
    void signupLoginNormalizationCsrfAndLogout() throws Exception {
        mvc.perform(post("/signup").param("name", "보안 테스트").param("email", "security@example.com")
                        .param("password", "password123").param("passwordConfirm", "password123"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/signup").with(csrf()).param("name", "보안 테스트")
                        .param("email", " SECURITY@example.com ").param("password", "password123")
                        .param("passwordConfirm", "password123"))
                .andExpect(redirectedUrl("/login?registered"));
        mvc.perform(post("/login").param("email", "security@example.com").param("password", "password123"))
                .andExpect(status().isForbidden());
        MockHttpSession anonymousSession = new MockHttpSession();
        String anonymousSessionId = anonymousSession.getId();
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/login").with(csrf()).session(anonymousSession)
                        .param("email", " SECURITY@EXAMPLE.COM ").param("password", "password123"))
                .andExpect(authenticated().withUsername("security@example.com"))
                .andExpect(redirectedUrl("/budget")).andReturn().getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getId()).isNotEqualTo(anonymousSessionId);
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/budget")).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void internalRoutesHaveNoCsrfExemption() throws Exception {
        mvc.perform(post("/internal/jobs/daily-notifications")).andExpect(status().isForbidden());
        mvc.perform(post("/internal/jobs/other")).andExpect(status().isForbidden());
        mvc.perform(post("/internal/jobs/daily-notifications/extra")).andExpect(status().isForbidden());
    }

    @Test
    void removedJobRemainsDeniedWithAuthenticationAndCsrf() throws Exception {
        mvc.perform(post("/internal/jobs/daily-notifications").with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("member@example.test")))
                .andExpect(status().isForbidden());
    }
}
