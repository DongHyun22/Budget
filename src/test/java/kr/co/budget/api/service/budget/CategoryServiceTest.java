package kr.co.budget.api.service.budget;

import kr.co.budget.domain.budget.Category;
import kr.co.budget.domain.budget.repository.CategoryRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import kr.co.budget.domain.member.Member;
import kr.co.budget.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CategoryServiceTest {
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final CategoryService service = new CategoryService(categories, members);

    @Test void normalizesCategoryAndRejectsDuplicateName() {
        when(members.findByIdForUpdate(7)).thenReturn(Optional.of(mock(Member.class)));
        service.create(7, "  COFFEE  ");
        verify(categories).save(argThat(c -> c.getName().equals("COFFEE") && c.getNormalizedName().equals("coffee")));
        var existing = mock(Category.class);
        when(existing.getId()).thenReturn(1L);
        when(categories.findByMemberIdAndNormalizedName(7, "coffee")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.create(7, "Coffee")).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
    @Test void forbidsCommonAndForeignCategoryUpdates() {
        when(members.findByIdForUpdate(7)).thenReturn(Optional.of(mock(Member.class)));
        for (Category category : new Category[]{new Category(null, "common"), new Category(8L, "foreign")}) {
            when(categories.findById(1L)).thenReturn(Optional.of(category));
            assertThatThrownBy(() -> service.update(7, 1, "renamed", false)).isInstanceOfSatisfying(ResponseStatusException.class,
                    ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
            assertThat(category.isActive()).isTrue();
        }
    }
    @Test void rejectsBlankAndOversizedNames() {
        assertThatThrownBy(() -> service.create(7, " ")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.create(7, "a".repeat(51))).isInstanceOf(ResponseStatusException.class);
    }
}
