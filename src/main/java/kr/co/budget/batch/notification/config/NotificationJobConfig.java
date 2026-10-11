package kr.co.budget.batch.notification.config;

import java.time.LocalDate;
import kr.co.budget.batch.notification.listener.NotificationJobListener;
import kr.co.budget.batch.notification.processor.NotificationProcessor;
import kr.co.budget.batch.notification.writer.NotificationWriter;
import kr.co.budget.domain.notification.Notification;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class NotificationJobConfig {
    @Bean
    public Job dailyNotificationJob(JobRepository repository, Step dailyNotificationStep,
                                    NotificationJobListener listener) {
        return new JobBuilder("dailyNotificationJob", repository)
                .validator(parameters -> {
                    try {
                        String value = parameters.getString("noticeDate");
                        if (value == null || !value.matches("[1-9][0-9]{3}-[0-9]{2}-[0-9]{2}")) {
                            throw new IllegalArgumentException("Expected YYYY-MM-DD");
                        }
                        LocalDate.parse(value);
                        if (parameters.getParameters().size() != 1
                                || !parameters.getParameter("noticeDate").isIdentifying()) {
                            throw new IllegalArgumentException("Only identifying noticeDate is supported");
                        }
                    } catch (RuntimeException ex) {
                        throw new JobParametersInvalidException("noticeDate must be an identifying YYYY-MM-DD date");
                    }
                })
                .listener(listener)
                .start(dailyNotificationStep)
                .build();
    }

    @Bean
    public Step dailyNotificationStep(JobRepository repository, PlatformTransactionManager transactionManager,
            JdbcPagingItemReader<Long> notificationMemberReader, NotificationProcessor processor,
            NotificationWriter writer) {
        return new StepBuilder("dailyNotificationStep", repository)
                .<Long, Notification>chunk(100, transactionManager)
                .reader(notificationMemberReader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
