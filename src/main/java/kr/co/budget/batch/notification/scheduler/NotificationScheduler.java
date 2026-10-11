package kr.co.budget.batch.notification.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.batch.notifications.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {
    private final JobLauncher jobLauncher;
    private final Job dailyNotificationJob;
    private final Clock clock;

    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public void notifyMembers() {
        var parameters = new JobParametersBuilder()
                .addString("noticeDate", LocalDate.now(clock).toString())
                .toJobParameters();
        try {
            jobLauncher.run(dailyNotificationJob, parameters);
        } catch (JobInstanceAlreadyCompleteException | JobExecutionAlreadyRunningException ex) {
            log.info("Daily notifications already completed or running: {}", parameters);
        } catch (Exception ex) {
            log.error("Could not launch daily notifications: {}", parameters, ex);
        }
    }
}
