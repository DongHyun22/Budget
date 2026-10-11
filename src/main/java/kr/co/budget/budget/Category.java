package kr.co.budget.budget;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.type.NumericBooleanConverter;

@Entity @Table(name = "category") @Getter @NoArgsConstructor
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_gen")
    @SequenceGenerator(name = "category_gen", sequenceName = "category_seq", allocationSize = 1)
    private Long id;
    private Long memberId;
    @Column(nullable = false, length = 50) private String name;
    @Column(nullable = false, length = 50) private String normalizedName;
    @Convert(converter = NumericBooleanConverter.class) @Column(nullable = false)
    private boolean active;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    public Category(Long memberId, String name) { this.memberId = memberId; change(name, true); }
    public void change(String name, boolean active) {
        this.name = name.trim(); this.normalizedName = this.name.toLowerCase(Locale.ROOT); this.active = active;
    }
    @PrePersist void insert() { createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")); }
}
