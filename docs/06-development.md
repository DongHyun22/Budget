# 개발자 안내

## 구조
```
src/main/java/kr/co/budget/
  config/          보안·시각 설정
  member/          회원과 인증
  budget/          예산·카테고리·지출·계산
  notification/    일일 알림과 배치
  common/          공통 예외
src/main/resources/db/migration/  Oracle Flyway SQL
src/main/webapp/WEB-INF/views/    JSP
src/main/resources/static/       CSS 테마·가계부 JavaScript 모듈
src/test/java/                   자동 테스트
infra/                          Docker·cron 도구
docs/                           설계·검증 문서
```

Controller는 요청과 화면 연결, Service는 소유권/업무 규칙, Repository는 DB 조회를 담당한다. 초기 화면은 서버가 만든 모델을 JSP/JSTL로 렌더링한다. 가계부는 vanilla JavaScript와 native dialog로 상호작용하며 `/budget/ui/**` 어댑터가 기존 서비스를 재사용한다. React·Thymeleaf·별도 프론트 빌드 도구는 사용하지 않는다. DB 자격증명은 로컬 .env에만 보관하고 Git에서 제외한다.

가계부 UI JSON의 금액/ID는 문자열로 전달한다. 브라우저 Number로 바꾸어 합산하지 않고 서버 결과를 표시한다. API 전용 오류는 JSON, 기존 MVC 오류는 HTML을 유지한다. 연도별 색상 변경은 [테마 관리](09-theme.md)를 참고한다.

## 실행 흐름
Docker Desktop Linux 엔진, JDK 17 필요. 로컬 초기화 도구로 .env 생성 후 Docker Compose의 oracle 서비스를 시작한다. DB health 확인 후 Gradle test/bootWar, oracle 프로필로 WAR 실행. 상세 명령은 루트 README와 구현 완료 후 검증 문서를 따른다.

## Toad
Oracle 연결의 Host 127.0.0.1, Port 11521, Service Name FREEPDB1, User BUDGET_APP. Password는 로컬 .env의 APP_USER_PASSWORD. SID FREE가 아니라 PDB 서비스로 연결한다. 이 PC의 1521/1522 바인딩이 차단되어 호스트 포트를 11521로 정했다. 컨테이너 내부는 1521이다. Toad 설치에 따라 호환 Oracle Client의 bitness가 필요할 수 있다. 앱 계정으로 테이블과 flyway_schema_history를 확인한다. SYS/SYSTEM은 초기 DB 관리용이며 앱 접속에 사용하지 않는다.

## 원본 이관 추적
- ETFGeniusGuide-server의 member는 이메일/이름/회원 ID 개념을 재사용하되 비밀번호와 인증을 새로 구현한다. 기존 네이버 mobile 값은 비밀번호가 아니다.
- budget의 월 계획·카테고리·지출 서비스 동작을 Oracle 스키마와 세션 회원 ID에 맞게 이관한다.
- ETFGeniusGuide-client의 BudgetPage/buildAllowance 계산을 서버 공통 계산기로 옮긴다.
- 원본 저장소와 .env, Git 이력, 실제 회원 데이터를 복사하지 않는다.

테스트용 H2를 쓰는 경우 단위/빠른 통합검증 보조에만 사용한다. Oracle 연결·DDL·JSP WAR의 실제 검증을 별도로 수행한다.
