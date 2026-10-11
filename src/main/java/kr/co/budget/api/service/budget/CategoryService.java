package kr.co.budget.api.service.budget;

import kr.co.budget.api.service.budget.validation.BudgetInput;
import kr.co.budget.domain.budget.Category;
import kr.co.budget.domain.budget.repository.CategoryRepository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import kr.co.budget.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categories;
    private final MemberRepository members;
    public List<Category> list(long memberId) { return categories.findByMemberIdIsNullOrMemberIdOrderByNameAsc(memberId); }
    public Category usable(long memberId, Long id, Long existingId) {
        if (id == null) throw BudgetInput.invalid("카테고리를 선택하세요.");
        var category = categories.findById(id).orElseThrow(BudgetInput::missing);
        if (category.getMemberId() != null && category.getMemberId() != memberId) throw BudgetInput.missing();
        if (!category.isActive() && !Objects.equals(id, existingId)) throw BudgetInput.invalid("비활성 카테고리는 새 지출에 사용할 수 없습니다.");
        return category;
    }
    @Transactional public Category create(long memberId, String name) {
        String clean = BudgetInput.name(name);
        members.findByIdForUpdate(memberId).orElseThrow(BudgetInput::missing);
        unique(memberId, clean, null);
        return categories.save(new Category(memberId, clean));
    }
    @Transactional public void update(long memberId, long id, String name, boolean active) {
        members.findByIdForUpdate(memberId).orElseThrow(BudgetInput::missing);
        var category = categories.findById(id).orElseThrow(BudgetInput::missing);
        if (!Objects.equals(category.getMemberId(), memberId)) throw BudgetInput.missing();
        String clean = BudgetInput.name(name);
        unique(memberId, clean, id);
        category.change(clean, active);
    }
    private void unique(long memberId, String name, Long id) {
        categories.findByMemberIdAndNormalizedName(memberId, name.toLowerCase(Locale.ROOT))
                .filter(other -> !Objects.equals(other.getId(), id))
                .ifPresent(other -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다."); });
    }
}
