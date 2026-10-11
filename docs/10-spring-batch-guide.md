# 오전 7시 전체 회원 알림 배치

Spring Boot 3.5.16이 관리하는 Spring Batch 5.2.6을 사용한다. 같은 앱 안에서 매일 한국시간 오전 7시에 실행하며, 모든 회원에게 웹 알림을 저장한다. 이메일·휴대폰 푸시는 포함하지 않는다.

## 파일과 역할

```text
batch/notification/
  config/NotificationJobConfig.java     Job과 chunk=100 Step 연결, 날짜 검증
  reader/MemberReaderConfig.java        회원 ID를 오름차순으로 100명씩 조회
  processor/NotificationProcessor.java  회원별 오늘 남은 금액 계산
  writer/NotificationWriter.java        알림 저장, 기존 회원·날짜는 건너뜀
  listener/NotificationJobListener.java 시작·종료·실패 로그
  scheduler/NotificationScheduler.java  오전 7시 JobLauncher 호출

domain/notification/
  Notification.java                    알림 Entity
  repository/NotificationRepository.java
api/service/notification/NotificationService.java
api/controller/notification/NotificationController.java
```

단순하게 회원 ID(Long) → Notification Entity를 전달하므로 별도 전달 DTO는 만들지 않았다. Reader는 JdbcPagingItemReader, Writer는 기존 JPA Repository를 사용하는 ItemWriter다.

## 실행 흐름

```text
Scheduler(07:00 Asia/Seoul) → JobLauncher → dailyNotificationJob
  → dailyNotificationStep
    → Reader: 회원 ID 1개
    → Processor: 기존 BudgetService.summary로 계산 → 알림 1개
    → Writer: 100개 단위 트랜잭션으로 저장
    → 마지막 100개 미만까지 처리
Listener: 시작·완료 상태·처리 수·실패 원인 기록
```

chunk=100은 100개를 한 트랜잭션으로 커밋한다는 뜻이다. 현재 Writer는 JPA로 하나씩 저장하며, 한 번의 SQL로 100개를 저장하는 JDBC 일괄 INSERT와는 다르다.

## 저장하는 금액

- 월 요약의 남은 금액이 아니라 **알림 날짜의 DayAllowance.remaining**이다.
- 일 기본액 = 월 예산 ÷ 해당 월 일수(정수 나눗셈).
- 오늘 남은 금액 = 오늘 배정액(전날 이월 포함) − 오늘 지출.
- 예: 1월 예산 31,000원, 1일 지출 1,200원, 2일 지출 1,200원 → 2일 남은 금액 -400원.
- 미래 날짜의 지출은 오늘 남은 금액에 영향을 주지 않는다.
- 월 예산이 없으면 remaining_amount=NULL로 저장하고 화면에 예산 설정 안내를 표시한다. 0원과 구분한다.
- 처리 당시 스냅샷이다. 이후 지출 수정으로 이미 저장된 알림을 변경하지 않는다.

## 설정과 DB

- `spring.batch.job.enabled=false`: 웹 앱 재시작 시 자동 실행하지 않는다.
- `spring.batch.jdbc.initialize-schema=never`: 운영 스키마는 Flyway가 관리한다.
- `app.batch.notifications.scheduling-enabled=false`: 예약 실행을 끈다. 기본값은 true다.
- V2: 실제 Batch 5.2.6 JAR의 공식 Oracle 메타데이터 SQL.
- V3: MEMBER_NOTIFICATION과 시퀀스. UNIQUE(member_id,notice_date).
- 기존 V1, DAILY_NOTIFICATION, notification_seq 및 기존 데이터는 보존한다. 이전 opening_amount를 새 remaining_amount로 해석하지 않는다.
- H2 테스트에서만 Batch 스키마 자동 초기화를 사용한다.

## 재시작과 운영

Job 식별자는 `noticeDate=YYYY-MM-DD` 하나다. 임의 timestamp나 run.id를 받지 않는다.

- 완료된 날짜: Spring Batch가 재실행을 거부한다.
- 실패한 날짜: 같은 noticeDate로 재시작하면 마지막 커밋 이후부터 이어간다.
- 한 회원 처리 실패: 현재 chunk 전체 롤백 후 Job 실패. 자동 skip/retry는 하지 않는다.
- 이미 저장된 회원·날짜: Writer가 건너뛰고 DB UNIQUE 제약도 중복을 방지한다.
- 오전 7시에 앱이 꺼져 있으면 자동 보충하지 않는다. 아래 명령으로 수동 실행한다.
- 실행 중 가입한 회원까지 동일 시점 스냅샷으로 고정하지 않는다. 작업 종료 후 가입한 회원은 다음 날부터 알림을 받는다.
- 강제 종료로 STARTED 상태가 남으면 프로세스 종료 여부를 확인한 뒤 운영자가 상태 복구해야 한다. 이 구현은 강제 종료된 실행을 자동 복구하지 않는다.

Docker 프로젝트 루트의 PowerShell에서 실행한다. 웹 앱의 8080과 충돌하지 않는 일회성 컨테이너다. 날짜는 실행할 날짜로 바꾼다.

```powershell
docker compose --profile app run --rm --no-deps app --spring.main.web-application-type=none --spring.batch.job.enabled=true --app.batch.notifications.scheduling-enabled=false noticeDate=2026-10-12
```

정상 종료는 코드0, Job 실패는 0이 아닌 코드로 종료한다. 재실행 시 과거 오전 7시의 상태를 복원하지는 않으며, 아직 저장되지 않은 회원은 재실행 당시 데이터를 계산한다.

알림 화면은 `/notifications`이며 본인 알림을 20개씩 최신순으로 조회한다. 읽음 처리는 POST `/notifications/{id}/read`와 CSRF 토큰이 필요하다. 기존 `/internal/jobs/daily-notifications` API와 작업 토큰은 사용하지 않는다.

## 검증

`NotificationBatchTest`: 251명, 예산 없음, 음수/오늘 지출/미래 지출, 두 번째 chunk 롤백, 동일 Job 재시작, 스냅샷 유지, 완료 날짜 중복 실행 차단, 잘못된 파라미터 거부.

`NotificationControllerTest`: 본인 목록만 조회, 비로그인 이동, 타인 읽음404, CSRF403, 읽음 반복 처리, 페이지 검증.

## 공식 자료

- [Spring Boot Batch 설정](https://docs.spring.io/spring-boot/3.5/how-to/batch.html)
- [Spring Batch 페이징 Reader](https://docs.spring.io/spring-batch/reference/5.2/readers-and-writers/database.html)
