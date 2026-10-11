# 운영 및 후속 배포 설계

## 일일 작업
Linux 호스트의 crontab이 매일 한국시간 00:05에 작업 스크립트를 호출한다. 스크립트는 POST /internal/jobs/daily-notifications에 X-Job-Token을 보낸다. 토큰은 권한 제한된 환경파일에 저장하고 URL/로그/Git에 넣지 않는다. 응답 오류는 비정상 종료, curl timeout 설정, flock 중복 프로세스 방지. DB 고유 제약으로 재실행 안전성 보장.

Linux 준비물은 bash, curl, flock(util-linux)이다. 호스트 시간대는 `timedatectl`로 Asia/Seoul인지 확인하고, 환경파일은 작업 사용자만 읽도록 `chmod 600 /opt/it-new-edu/.env`를 적용한다. 실행 계정에 로그 디렉터리 쓰기 권한을 부여한다. 배포 경로에 맞춰 `infra/crontab.example`을 수정한 후 `crontab -e`로 등록하고 `crontab -l`로 확인한다. 최초에는 동일 스크립트를 수동 호출하여 JSON 응답을 확인한다. cron 등록은 이 Windows 개발 환경에서 자동 수행하지 않았다.

당일 알림만 생성하며 중단 후 복구 시 수동 재실행으로 당일 보완. 화면 계산은 배치와 무관하게 최신 지출을 반영한다. 예산 미설정 회원은 알림 없음. 처음 생성된 금액을 보존하므로 예산/과거 지출 수정 후 값이 달라도 알림은 생성 당시 값임을 표시한다.

## 향후 CI/CD
GitLab push → Jenkins checkout → 테스트·WAR → SonarQube 분석/Quality Gate → Docker build → Registry push → AWS Linux에서 이미지 digest 배포 → health·화면 smoke → 실패 시 이전 이미지 복귀. DB 변경은 하위 호환을 유지하며 이미지 롤백과 DB 롤백을 동일시하지 않는다. DB 백업/복구는 배포 이전 확인.

처음에는 앱과 DB를 별도 컨테이너/영속 볼륨으로 구성한다. Oracle DB 포트를 외부 공개하지 않으며 앱은 HTTPS 프록시 뒤에서 운영하고 세션 쿠키 Secure를 설정한다. 실제 EC2·Registry·GitLab·Jenkins 접근과 운영 비밀은 다음 단계에서 연결한다.

## 모니터링
Jennifer와 Scouter는 사용자 희망 스택으로 남긴다. 처음엔 하나씩 별도 환경에서 연결해 JVM 버전·agent 지원 및 성능 영향을 검증한 후 동시 사용을 결정하는 것을 권장한다. 라이선스/수집 서버/agent 파일은 저장소에 포함하지 않는다. agent를 외부 마운트하고 JAVA_TOOL_OPTIONS로 연결하는 방식을 검토한다. 로그인 비밀번호·세션 쿠키·작업 토큰·개인 메모 수집을 피하도록 수집 설정을 확인한다.

## 지금 구현하지 않는 운영 단계
AWS 자원 생성, GitLab 원격 등록, 실제 Jenkins/SonarQube/Jennifer/Scouter 서버 설치는 로컬 동작 검증 이후 진행한다. 이 문서는 배포 완료 보고가 아니다.
