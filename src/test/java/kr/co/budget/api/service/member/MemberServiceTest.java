package kr.co.budget.api.service.member;

import kr.co.budget.domain.member.Member;
import kr.co.budget.domain.member.repository.MemberRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import kr.co.budget.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class MemberServiceTest {
    private final MemberRepository repository = mock(MemberRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private MemberService service;

    @BeforeEach
    void setup() {
        service = new MemberService(repository, encoder,
                Clock.fixed(Instant.parse("2026-09-19T00:00:00Z"), ZoneId.of("Asia/Seoul")));
        when(repository.saveAndFlush(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void signupNormalizesEmailAndStoresOnlyPasswordHash() {
        Member member = service.signup(" 사용자 ", " USER@Example.com ", "password123", "password123");
        assertThat(member.getEmail()).isEqualTo("user@example.com");
        assertThat(member.getName()).isEqualTo("사용자");
        assertThat(member.getPasswordHash()).isNotEqualTo("password123");
        assertThat(encoder.matches("password123", member.getPasswordHash())).isTrue();
        assertThat(member.getCreatedAt().getHour()).isEqualTo(9);
    }

    @Test
    void signupRejectsDuplicateNormalizedEmail() {
        when(repository.existsByEmail("user@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.signup("사용자", " USER@Example.com ", "password123", "password123"))
                .isInstanceOf(AppException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void signupRejectsMismatchShortPasswordsAndBcryptByteOverflow() {
        assertThatThrownBy(() -> service.signup("사용자", "user@example.com", "password123", "different"))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.signup("사용자", "user@example.com", "short", "short"))
                .isInstanceOf(AppException.class);
        String multibyte = "가".repeat(25);
        assertThatThrownBy(() -> service.signup("사용자", "user@example.com", multibyte, multibyte))
                .isInstanceOf(AppException.class);
        verify(repository, never()).saveAndFlush(any());
    }
}
