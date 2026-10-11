package kr.co.budget.batch.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import kr.co.budget.batch.notification.writer.NotificationWriter;
import kr.co.budget.domain.member.Member;
import kr.co.budget.domain.member.repository.MemberRepository;
import kr.co.budget.domain.notification.Notification;
import kr.co.budget.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:notification-batch;MODE=Oracle;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class NotificationBatchTest {
    @Autowired JobLauncher launcher;
    @Autowired Job dailyNotificationJob;
    @Autowired MemberRepository members;
    @Autowired NotificationRepository notifications;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean NotificationWriter writer;

    @Test
    void failedChunkRollsBackAndRestartsWithoutDuplicatesOrChangingSnapshots() throws Exception {
        var targets = new ArrayList<Member>();
        for (int i = 0; i < 251; i++) {
            targets.add(new Member("batch" + i + "@example.test", "hash", "batch", LocalDateTime.now()));
        }
        targets = new ArrayList<>(members.saveAllAndFlush(targets));
        long budgetMember = targets.get(0).getId();
        LocalDate date = LocalDate.of(2027, 1, 2);
        jdbc.update("insert into budget_plan (id,member_id,ym,amount,created_at,updated_at) values (budget_plan_seq.nextval,?,'2027-01',31000,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", budgetMember);
        jdbc.update("insert into category (id,name,normalized_name,active,created_at,updated_at) values (category_seq.nextval,'batch','batch',1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        Long category = jdbc.queryForObject("select id from category where name='batch'", Long.class);
        for (int day : new int[]{1, 2, 3}) {
            jdbc.update("insert into spend (id,member_id,spend_date,category_id,amount,memo,created_at,updated_at) values (spend_seq.nextval,?,?,?,?,NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                    budgetMember, java.sql.Date.valueOf(date.withDayOfMonth(day)), category, day == 3 ? 9000 : 1200);
        }
        var params = new JobParametersBuilder().addString("noticeDate", date.toString()).toJobParameters();
        var failOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            Chunk<? extends Notification> chunk = invocation.getArgument(0);
            invocation.callRealMethod();
            if (chunk.getItems().get(0).getMemberId() > budgetMember + 99 && failOnce.getAndSet(false)) {
                throw new IllegalStateException("Simulated failure after writing second chunk");
            }
            return null;
        }).when(writer).write(any());

        var failed = launcher.run(dailyNotificationJob, params);
        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(notifications.count()).isEqualTo(100);
        var first = notifications.findByMemberIdOrderByNoticeDateDesc(budgetMember,
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent().get(0);
        assertThat(first.getRemainingAmount()).isEqualTo(-400);
        jdbc.update("update spend set amount=1 where member_id=?", budgetMember);
        reset(writer);

        var restarted = launcher.run(dailyNotificationJob, params);
        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restarted.getJobInstance().getInstanceId()).isEqualTo(failed.getJobInstance().getInstanceId());
        assertThat(restarted.getStepExecutions().iterator().next().getReadCount()).isEqualTo(151);
        assertThat(notifications.count()).isEqualTo(251);
        assertThat(notifications.findById(first.getId()).orElseThrow().getRemainingAmount()).isEqualTo(-400);
        assertThat(jdbc.queryForObject("select count(*) from member_notification where remaining_amount is null", Long.class)).isEqualTo(250);
        assertThatThrownBy(() -> launcher.run(dailyNotificationJob, params)).isInstanceOf(JobInstanceAlreadyCompleteException.class);
        assertThat(notifications.count()).isEqualTo(251);
    }

    @Test
    void rejectsMissingInvalidAndAdditionalIdentifyingParameters() {
        for (var params : new org.springframework.batch.core.JobParameters[]{
                new JobParametersBuilder().toJobParameters(),
                new JobParametersBuilder().addString("noticeDate", "2027-02-30").toJobParameters(),
                new JobParametersBuilder().addString("noticeDate", "2027-01-01").addLong("run.id", 1L).toJobParameters(),
                new JobParametersBuilder().addString("noticeDate", "2027-01-01", false).toJobParameters()}) {
            assertThatThrownBy(() -> launcher.run(dailyNotificationJob, params))
                    .isInstanceOf(JobParametersInvalidException.class);
        }
    }
}
