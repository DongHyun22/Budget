# 기획서

## 목적
매월 변동지출 예산을 정하고 하루 사용 가능 금액과 이월액을 확인하는 회원 전용 가계부. 기존 ETFGeniusGuide의 가계부 규칙을 JSP 기반 서버 렌더링으로 이관한다.

## 사용자와 범위
일반 회원이 자기 예산·카테고리·지출·알림을 관리한다. 이메일/비밀번호 가입 및 세션 로그인. 주식, ETF, KIS, 네이버 인증, 투자 계산, 이메일 발송, 관리자 업무, 결제는 포함하지 않는다. 기존 운영 회원/개인정보 DB를 자동 복사하지 않으며 신규 계정으로 시작한다.

## 기술
Java 17 / Spring Boot 3.5.16 / Gradle / Spring MVC·Security·Data JPA / JSP·JSTL / Tomcat 실행 WAR / Oracle Free Docker. DB 변경은 Flyway SQL, 개발자 조회·관리는 Toad. 원본 구조는 서비스와 저장소 계층으로 이어가되 Oracle과 새 인증에 맞게 수정한다.

## 단계
1. 설계 문서 및 로컬 회원·가계부·알림 구현.
2. Oracle 실제 저장과 브라우저 폼 검증.
3. GitLab 저장소 확정 후 등록.
4. Jenkins 빌드·테스트·SonarQube 검사, Docker 이미지 생성, AWS Linux 배포.
5. Jennifer/Scouter 연동 검증 및 운영 절차 확정.

배포 전까지 개인 PC 로컬 개발 환경이다. 비밀번호 재설정 이메일·이메일 소유 검증·운영 로그인 공격 방어 정책은 운영 공개 전에 별도 설계한다.
