package kr.co.budget.budget;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpendRepository extends JpaRepository<Spend, Long> {
    Optional<Spend> findByIdAndMemberId(long id, long memberId);
    List<Spend> findAllByMemberIdAndSpendDateBetweenOrderBySpendDateAscIdAsc(long memberId, LocalDate start, LocalDate end);
}
