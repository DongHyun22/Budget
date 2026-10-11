# 하루예산 · 변동지출 가계부

ETFGeniusGuide에서 회원과 변동지출 가계부 동작을 이관한 Spring Boot/JSP 프로젝트. 자체 이메일 회원가입·세션 로그인, 월 예산, 공용/개인 카테고리, 지출 관리, 일일 사용 가능 금액 알림을 제공한다.

## 기술 및 문서
Java 17 · Spring Boot 3.5.16 · Gradle 8.8 · Spring MVC/Security/JPA · JSP/JSTL · Tomcat 실행 WAR · Oracle Free · Flyway.

[개발 산출물 목차](docs/README.md)에서 기획서, 기능명세, 화면정의, HTTP/API 명세, ERD/테이블정의, 개발자 안내, 배포·운영 문서를 확인한다. React/Thymeleaf/KIS/ETF/네이버 로그인은 사용하지 않는다.

## Windows 로컬 실행
Docker Desktop을 Linux 컨테이너 모드로 실행하고 JDK 17을 준비한다. 프로젝트 폴더의 PowerShell에서:

```powershell
# 최초 한 번: 무작위 개발 자격증명을 .env에 생성 (기존 파일 보존)
.\infra\init-local.ps1
docker compose up -d oracle
docker compose ps
# oracle이 healthy가 되면 테스트, WAR 빌드, 앱 실행
.\infra\run-local.ps1 -Test -Build
```

브라우저에서 http://localhost:8080 에 접속해 회원가입한다. 앱 종료는 Ctrl+C. DB 종료는 `docker compose stop oracle`. `docker compose down -v`는 DB 데이터를 삭제하므로 사용하지 않는다.

`run-local.ps1`은 이 PC의 `C:\Program Files\Java\jdk-17`을 우선 사용한다. 다른 PC에서는 JAVA_HOME을 JDK 17 이상으로 설정한다. `.env`의 비밀은 출력하거나 Git에 올리지 않는다. 로컬 계정 비밀번호는 로그인 화면에서 직접 설정한다.

## Toad 접속
Host `127.0.0.1`, Port `11521`, Service Name `FREEPDB1`, User `BUDGET_APP`. Password는 `.env`의 `APP_USER_PASSWORD`. Docker 컨테이너 내부 포트는 1521이다. SYS/SYSTEM으로 애플리케이션을 실행하지 않는다.

## 자동 테스트·패키징
```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat test bootWar
```

테스트는 H2 Oracle 호환 모드의 빠른 검증을 포함한다. 실제 Oracle 검증과 결과는 `docs/08-verification.md`에 별도로 기록한다. 결과물 `build/libs/budget.war`, 테스트 보고서 `build/reports/tests/test/index.html`, 커버리지 `build/reports/jacoco/test/html/index.html`.

## 앱도 Docker로 실행
WAR 빌드 후 로컬 Java 앱을 종료하고:
```powershell
docker compose --profile app up -d --build
```
DB는 별도 영속 볼륨, 웹은 localhost:8080으로 연결한다. 이 Docker 설정은 로컬 개발용이며 운영 배포 완료를 의미하지 않는다.

## Linux 일일 알림
`infra/daily-notifications.sh`와 `infra/crontab.example`을 사용한다. 한국시간 00:05 실행, 회원·날짜별 중복 방지, 월 예산 없는 회원 생략. 처음 생성된 하루 시작 금액을 알림에 보존하고 가계부 화면은 최신 지출을 반영한다. [운영 문서](docs/07-operations.md)의 토큰·시간대·로그 설정을 먼저 읽는다.

GitLab 등록, Jenkins/SonarQube 연결, AWS 배포, Jennifer/Scouter 서버 연결은 다음 단계에서 진행한다.
