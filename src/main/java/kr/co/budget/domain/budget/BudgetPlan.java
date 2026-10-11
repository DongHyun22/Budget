package kr.co.budget.domain.budget;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "budget_plan") @Getter @NoArgsConstructor
public class BudgetPlan {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "plan_seq")
    @SequenceGenerator(name = "plan_seq", sequenceName = "budget_plan_seq", allocationSize = 1)
    private Long id;
    @Column(nullable = false) private Long memberId;
    @Column(nullable = false, length = 7) private String ym;
    @Column(nullable = false, precision = 14, scale = 0) private Long amount;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    public BudgetPlan(long memberId, String ym, long amount) {
        this.memberId = memberId; this.ym = ym; this.amount = amount;
    }
    public void changeAmount(long amount) { this.amount = amount; }
    @PrePersist void insert() { createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")); }
}
