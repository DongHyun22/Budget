package kr.co.budget.notification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {
    private final NotificationJob job;
    private final Clock clock;
    private final String token;

    public JobController(NotificationJob job, Clock clock, @Value("${app.jobs.token:}") String token) {
        this.job = job;
        this.clock = clock;
        this.token = token;
    }

    @PostMapping("/internal/jobs/daily-notifications")
    public ResponseEntity<JobResult> run(@RequestHeader(value = "X-Job-Token", required = false) String supplied) {
        if (token.length() < 32 || supplied == null || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(401).build();
        }
        LocalDate date = LocalDate.now(clock);
        return ResponseEntity.ok(new JobResult(date, job.generate(date)));
    }

    public record JobResult(LocalDate date, int created) { }
}
