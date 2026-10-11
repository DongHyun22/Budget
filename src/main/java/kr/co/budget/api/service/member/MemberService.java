package kr.co.budget.api.service.member;

import kr.co.budget.domain.member.Member;
import kr.co.budget.domain.member.repository.MemberRepository;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import kr.co.budget.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final MemberRepository members;
    private final PasswordEncoder passwords;
    private final Clock clock;

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public Member signup(String name, String email, String password, String passwordConfirm) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = normalizeEmail(email);
        if (normalizedName.isBlank() || normalizedName.length() > 50) {
            throw new AppException(HttpStatus.BAD_REQUEST, "이름은 1~50자로 입력해 주세요.");
        }
        if (normalizedEmail.length() > 254 || !EMAIL.matcher(normalizedEmail).matches()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "올바른 이메일 주소를 입력해 주세요.");
        }
        if (password == null || password.length() < 8 || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AppException(HttpStatus.BAD_REQUEST, "비밀번호는 8~64자, UTF-8 기준 72바이트 이하로 입력해 주세요.");
        }
        if (!password.equals(passwordConfirm)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다.");
        }
        if (members.existsByEmail(normalizedEmail)) {
            throw duplicateEmail();
        }
        try {
            return members.saveAndFlush(new Member(normalizedEmail, passwords.encode(password),
                    normalizedName, LocalDateTime.now(clock)));
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmail();
        }
    }

    private AppException duplicateEmail() {
        return new AppException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        Member member = members.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("이메일 또는 비밀번호를 확인해 주세요."));
        return User.withUsername(member.getEmail()).password(member.getPasswordHash()).roles("MEMBER").build();
    }

    @Transactional(readOnly = true)
    public long requireMemberId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return members.findByEmail(normalizeEmail(authentication.getName())).map(Member::getId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."));
    }
}
