package kr.co.budget.batch.notification.reader;

import java.util.Map;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemberReaderConfig {
    @Bean
    @StepScope
    public JdbcPagingItemReader<Long> notificationMemberReader(DataSource dataSource) {
        return new JdbcPagingItemReaderBuilder<Long>()
                .name("notificationMemberReader")
                .dataSource(dataSource)
                .selectClause("id")
                .fromClause("app_member")
                .sortKeys(Map.of("id", Order.ASCENDING))
                .rowMapper((rs, rowNum) -> rs.getLong("id"))
                .pageSize(100)
                .build();
    }
}
