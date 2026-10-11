package kr.co.budget.batch.notification.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationJobListener implements JobExecutionListener {
    @Override
    public void beforeJob(JobExecution execution) {
        log.info("Daily notifications started: execution={}, date={}", execution.getId(),
                execution.getJobParameters().getString("noticeDate"));
    }

    @Override
    public void afterJob(JobExecution execution) {
        long written = execution.getStepExecutions().stream().mapToLong(step -> step.getWriteCount()).sum();
        log.info("Daily notifications finished: execution={}, status={}, processed={}",
                execution.getId(), execution.getStatus(), written);
        execution.getAllFailureExceptions().forEach(error -> log.error("Daily notification job failed", error));
    }
}
