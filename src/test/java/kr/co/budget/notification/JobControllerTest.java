package kr.co.budget.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class JobControllerTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-18T15:05:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void rejectsMissingOrWrongTokenBeforeDoingWork() {
        NotificationJob job = mock(NotificationJob.class);
        JobController controller = new JobController(job, clock, "0123456789abcdef0123456789abcdef");
        assertThat(controller.run(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.run("wrong").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(job);
    }

    @Test
    void blankConfiguredTokenNeverEnablesJob() {
        NotificationJob job = mock(NotificationJob.class);
        JobController controller = new JobController(job, clock, "");
        assertThat(controller.run("").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(job);
    }

    @Test
    void usesKoreanDateAndReturnsCreatedCount() {
        NotificationJob job = mock(NotificationJob.class);
        LocalDate expected = LocalDate.of(2026, 9, 19);
        when(job.generate(expected)).thenReturn(2);
        String token = "0123456789abcdef0123456789abcdef";
        JobController controller = new JobController(job, clock, token);
        assertThat(controller.run(token).getBody()).isEqualTo(new JobController.JobResult(expected, 2));
        verify(job).generate(expected);
    }
}
