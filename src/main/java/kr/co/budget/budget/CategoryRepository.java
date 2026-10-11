package kr.co.budget.budget;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByMemberIdIsNullOrMemberIdOrderByNameAsc(long memberId);
    Optional<Category> findByMemberIdAndNormalizedName(long memberId, String normalizedName);
}
