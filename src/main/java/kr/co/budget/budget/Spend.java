package kr.co.budget.budget;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "spend") @Getter @NoArgsConstructor
public class Spend {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "spend_gen")
    @SequenceGenerator(name = "spend_gen", sequenceName = "spend_seq", allocationSize = 1)
    private Long id;
    @Column(nullable = false) private Long memberId;
    @Column(nullable = false) private LocalDate spendDate;
    @Column(nullable = false) private Long categoryId;
    @Column(nullable = false, precision = 14, scale = 0) private Long amount;
    @Column(length = 200) private String memo;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    public Spend(long memberId, LocalDate date, long categoryId, long amount, String memo) {
        this.memberId = memberId; this.spendDate = date; change(categoryId, amount, memo);
    }
    public void change(long categoryId, long amount, String memo) {
        this.categoryId = categoryId; this.amount = amount; this.memo = memo;
    }
    @PrePersist void insert() { createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")); }
}
