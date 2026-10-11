package kr.co.budget.notification;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationJob {
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;

    public int generate(LocalDate date) {
        int created = 0;
        long afterId = 0;
        while (true) {
            List<Long> ids = jdbc.queryForList(
                    "select member_id from budget_plan where ym = ? and member_id > ? order by member_id fetch first 100 rows only",
                    Long.class, YearMonth.from(date).toString(), afterId);
            if (ids.isEmpty()) {
                return created;
            }
            for (long id : ids) {
                if (notifications.generateForMember(id, date)) {
                    created++;
                }
            }
            afterId = ids.get(ids.size() - 1);
        }
    }
}
