# 오전 7시 전체 회원 알림 배치 구현 안내

이 문서는 설계 안내다. 기존 알림 구현은 제거했으며 아래 클래스, 의존성, 예약 실행은 아직 추가하지 않았다.

## 목표와 범위

- 매일 한국시간 오전 7시 전체 회원을 처리한다. 월 예산이 없는 회원도 포함한다.
- 먼저 회원별 알림 레코드를 DB에 저장하는 방식으로 배우는 것을 제안한다. 이메일·푸시 실제 발송은 별도 발송 서비스가 필요하다.
- 알림 문구와 전달 채널은 후속 구현 전에 정한다. DB 저장만으로 휴대폰 푸시나 이메일이 전송되지는 않는다.
- Spring Batch는 처리 엔진이고, 오전 7시에 시작시키는 것은 Scheduler의 역할이다.

## 제안하는 폴더

별도 서버나 Gradle 모듈 없이 같은 앱에 batch/notification 패키지를 둔다. 기존 Java 구조와 동일하게 역할별 하위 패키지로 구분한다. 아래 구조는 후속 구현 때 생성한다.

```text
src/main/java/kr/co/budget/batch/notification/
  config/NotificationJobConfig.java      Job과 Step을 Bean으로 연결
  scheduler/NotificationScheduler.java   오전 7시에 JobLauncher 호출
  reader/MemberReaderConfig.java         JdbcPagingItemReader Bean
  processor/NotificationProcessor.java  회원 한 명 → 저장할 알림 DTO
  writer/NotificationWriterConfig.java   JdbcBatchItemWriter Bean
  listener/NotificationJobListener.java  시작·종료·성공·실패 로그
  dto/NotificationTarget.java            회원 조회 DTO
  dto/NotificationDraft.java             알림 저장 DTO
```

Reader와 Writer는 Spring이 제공하는 JDBC 구현을 Bean으로 설정하면 된다. 세 클래스를 반드시 직접 구현해야 하는 것은 아니다. Processor만 `ItemProcessor<NotificationTarget, NotificationDraft>`로 작성하면 시작하기 쉽다.

## 실행 순서

```text
Scheduler(07:00) → JobLauncher → Job → Step
                                     Reader: 회원 1명 읽기
                                     Processor: 알림 1개 만들기
                                     100개 모이면 Writer: 묶어서 DB 저장·커밋
                                     끝까지 반복 (마지막 100개 미만도 저장)
Listener: Job 시작과 종료 결과 기록
```

예를 들어 회원 250명, chunk 크기 100이면 100명 / 100명 / 50명으로 저장한다. 한 chunk가 실패하면 그 chunk의 DB 트랜잭션이 롤백된다. 이미 커밋한 앞 chunk까지 모두 되돌리는 것은 아니다.

## 구현 순서

1. `build.gradle`에 `implementation 'org.springframework.boot:spring-boot-starter-batch'`를 추가한다. 현재 Boot 3.5가 관리하는 Batch 5 계열을 사용하고 별도 버전을 지정하지 않는다.
2. Flyway의 새 버전에 Batch 메타데이터 테이블(`BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION` 등)과 알림 저장 구조를 준비한다. Oracle용 스키마는 실제 해결된 Batch 버전의 공식 SQL을 사용한다. 적용된 V1은 수정하지 않는다.
3. `spring.batch.job.enabled: false`로 앱 시작 때 Job이 자동 실행되지 않도록 한다. Flyway로 메타데이터를 관리한다면 `spring.batch.jdbc.initialize-schema: never`로 둔다. Boot 자동 설정을 쓸 때 `@EnableBatchProcessing`은 붙이지 않는다.
4. Reader는 `APP_MEMBER`를 `id ASC`로 페이징 조회한다. 고유한 id를 정렬 키로 사용하고 예산 테이블과 INNER JOIN하지 않는다. 실행마다 상태가 새로 만들어지도록 `@StepScope`를 사용한다.
5. Processor는 회원 한 명의 알림 DTO를 만든다. 전체 회원에게 알림을 만들 목적이므로 예산이 없다는 이유로 `null`을 반환해 제외하지 않는다.
6. Writer는 DTO를 알림 테이블에 일괄 저장한다. 회원+업무일자+알림종류의 고유 제약과 중복 시 안전하게 건너뛰는 SQL로 재실행을 보호한다.
7. Config에서 chunk 크기 100의 Step을 만들고 Job에 연결한다. Job에는 Listener를 연결한다.
8. Scheduler에서 `@Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")`로 JobLauncher를 호출한다. 스케줄러 설정에는 `@EnableScheduling`을 둔다.

## Job과 Step 연결 예시 (Batch 5, 설명용)

아래는 구성 메서드의 핵심이며 완성된 실행 클래스가 아니다. Reader, Processor, Writer, Listener 및 저장 구조를 먼저 정의해야 한다.

```java
@Bean
Step notificationStep(JobRepository repository,
        PlatformTransactionManager transactionManager,
        ItemReader<NotificationTarget> reader,
        ItemProcessor<NotificationTarget, NotificationDraft> processor,
        ItemWriter<NotificationDraft> writer) {
    return new StepBuilder("notificationStep", repository)
            .<NotificationTarget, NotificationDraft>chunk(100, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
}

@Bean
Job notificationJob(JobRepository repository, Step notificationStep,
        NotificationJobListener listener) {
    return new JobBuilder("notificationJob", repository)
            .start(notificationStep)
            .listener(listener)
            .build();
}
```

`JobRepository`는 업무 알림을 저장하는 저장소가 아니라 배치 실행 상태·재시작 위치를 기록하는 저장소다. Listener에는 시작·종료 로그만 두고, 회원별 알림 생성은 Processor/Writer에 둔다.

Scheduler는 `businessDate=2026-10-12`처럼 한국시간 기준 날짜를 식별 JobParameter로 전달한다. 같은 날짜의 완료된 Job은 다시 실행되지 않고, 실패한 Job은 같은 파라미터로 재시작한다. 매번 임의 timestamp를 식별값에 붙이면 다른 JobInstance가 되어 이 보호가 사라진다.

오전 7시에 앱이 꺼져 있으면 `@Scheduled`는 놓친 실행을 자동 보충하지 않는다. 초기에는 앱 한 대로 운영하고, 놓친 날짜의 재실행과 여러 앱 인스턴스의 중복 예약은 후속 운영 정책으로 정한다. 외부 이메일·푸시는 DB 롤백으로 취소되지 않으므로 실제 발송으로 확장할 때 발송 대기 기록(outbox)과 중복 발송 방지가 필요하다.

## 최소 검증

- 250명 → 250개, 마지막 50개도 저장.
- 예산이 없는 회원도 알림 생성.
- 같은 업무일자를 재실행해도 중복 없음.
- 두 번째 chunk 실패 후 동일 파라미터 재시작 시 누락·중복 없음.
- 회원 0명 정상 종료, 한국시간 오전 7시 예약 설정 확인.

## 공식 자료

- [Reader·Processor·Writer (Batch 5.2)](https://docs.spring.io/spring-batch/reference/5.2/readersAndWriters.html)
- [Chunk Step 구성 (Batch 5.2)](https://docs.spring.io/spring-batch/reference/5.2/step/chunk-oriented-processing/configuring.html)
- [Boot 3.5 Batch 설정](https://docs.spring.io/spring-boot/3.5/how-to/batch.html)
- [Spring 예약 실행](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
