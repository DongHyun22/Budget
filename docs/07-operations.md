# 운영 및 후속 배포 설계

## 일일 작업 전환
기존 내부 알림 API, 전용 작업 토큰 설정, cron 스크립트는 제거했다. 외부에 등록했던 예약 작업이 있다면 운영 환경에서도 해제한다. Spring Batch가 매일 Asia/Seoul 오전 7시에 전체 회원을 chunk=100으로 처리한다. 놓친 실행·실패 재시작은 [수동 실행 안내](10-spring-batch-guide.md)를 따른다. 오전 7시에 앱이 실행 중이어야 한다.

기존 알림 테이블·데이터와 적용된 V1은 유지한다. V2는 Batch 메타데이터, V3는 MEMBER_NOTIFICATION을 추가한다. 실행 결과는 BATCH_JOB_EXECUTION.STATUS와 앱 로그에서 확인한다. 완료한 날짜는 다시 실행하지 않으며, 실패한 날짜는 같은 noticeDate로 재시작한다. 강제 종료로 STARTED 상태가 남은 경우 자동 복구하지 않는다.

## 향후 CI/CD
GitLab push → Jenkins checkout → 테스트·WAR → SonarQube 분석/Quality Gate → Docker build → Registry push → AWS Linux에서 이미지 digest 배포 → health·화면 smoke → 실패 시 이전 이미지 복귀. DB 변경은 하위 호환을 유지하며 이미지 롤백과 DB 롤백을 동일시하지 않는다. DB 백업/복구는 배포 이전 확인.

처음에는 앱과 DB를 별도 컨테이너/영속 볼륨으로 구성한다. Oracle DB 포트를 외부 공개하지 않으며 앱은 HTTPS 프록시 뒤에서 운영하고 세션 쿠키 Secure를 설정한다. 실제 EC2·Registry·GitLab·Jenkins 접근과 운영 비밀은 다음 단계에서 연결한다.

## 모니터링
Jennifer와 Scouter는 사용자 희망 스택으로 남긴다. 처음엔 하나씩 별도 환경에서 연결해 JVM 버전·agent 지원 및 성능 영향을 검증한 후 동시 사용을 결정하는 것을 권장한다. 라이선스/수집 서버/agent 파일은 저장소에 포함하지 않는다. agent를 외부 마운트하고 JAVA_TOOL_OPTIONS로 연결하는 방식을 검토한다. 로그인 비밀번호·세션 쿠키·개인 메모 수집을 피하도록 수집 설정을 확인한다.

## 지금 구현하지 않는 운영 단계
AWS 자원 생성, GitLab 원격 등록, 실제 Jenkins/SonarQube/Jennifer/Scouter 서버 설치는 로컬 동작 검증 이후 진행한다. 이 문서는 배포 완료 보고가 아니다.
