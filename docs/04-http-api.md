# HTTP/API 명세

초기 화면은 JSP 서버 렌더링이다. 가계부 모달은 같은 세션으로 `/budget/ui/**` JSON 응답을 사용한다. 기존 HTML 화면/폼 경로도 유지한다. 사용자 변경 요청에 Spring Security CSRF가 필요하다. 날짜 yyyy-MM-dd, 월 yyyy-MM, 금액 원 단위 정수. 로그인 후 JSESSIONID 쿠키를 사용한다.

| 메서드 | URL | 입력 | 결과 |
|---|---|---|---|
| GET | / | 없음 | 인증 시 /budget, 비인증 시 /login 이동 |
| GET/POST | /signup | POST name,email,password,passwordConfirm | 가입 화면/성공 시 /login?registered |
| GET/POST | /login | POST email,password | 로그인 화면/성공 시 /budget, 실패 /login?error |
| POST | /logout | CSRF | 세션 종료, 로그인 이동 |
| GET | /budget | ym 선택 | 월 가계부 HTML |
| POST | /budget/plan | ym,amount | 예산 저장, 해당 월 이동 |
| POST | /budget/spends | date,categoryId,amount,memo | 지출 등록, 해당 월 이동 |
| GET | /budget/spends/{id} | 지출 ID | 본인 상세 HTML |
| POST | /budget/spends/{id} | categoryId,amount,memo | 지출 수정, 해당 월 이동 |
| POST | /budget/spends/{id}/delete | 지출 ID | 삭제, 해당 월 이동 |
| GET | /categories | 없음 | 본인/공용 목록 HTML |
| POST | /categories | name | 개인 카테고리 생성 |
| POST | /categories/{id} | name,active | 개인 이름/활성 상태 변경 |
| GET | /notifications | page 선택(0부터) | 본인 알림 20개씩 최신순 HTML |
| POST | /notifications/{id}/read | 알림 ID, CSRF | 본인 알림 읽음 처리 후 목록 이동 |
| GET | /actuator/health | 없음 | 공개 상태만, 내부 상세 미공개 |

비로그인은 로그인으로 이동, CSRF 누락 403, 본인 소유가 아닌 지출·알림은 404, 공용/타인 카테고리 수정은 거부. 검증 실패는 400 안내, 중복 입력은 409 또는 해당 폼 오류로 처리한다. 기존 내부 알림 실행 API와 내부 경로의 보안 예외는 복구하지 않는다. 배치는 앱 스케줄러 또는 Docker 일회 실행으로 시작한다.

## 가계부 모달용 JSON 계약

POST는 `application/x-www-form-urlencoded`, 응답은 아래와 같다. CSRF 헤더명/토큰은 JSP가 제공하는 메타데이터에서 읽는다. 클라이언트가 memberId를 지정하지 않으며 서버가 인증 회원을 결정한다.

| 메서드 | URL | 입력 | 성공 |
|---|---|---|---|
| GET | /budget/ui/summary | ym | 200 월 요약 JSON |
| POST | /budget/ui/plan | ym,amount | 204 |
| POST | /budget/ui/spends | date,categoryId,amount,memo | 204 |
| POST | /budget/ui/spends/{id} | categoryId,amount,memo | 204 (날짜 변경 없음) |
| POST | /budget/ui/spends/{id}/delete | 없음 | 204 |
| POST | /budget/ui/categories | name | 201 `{id,name,active}` |

요약: `ym, planAmount, baseAmount, totalSpent, remaining, unallocated, days, spends, categories, today`.

- `days`: `{date,base,allowed,spent,remaining}`.
- `spends`: `{id,date,categoryId,categoryName,amount,memo}`.
- `categories`: `{id,name,active}`. 비활성 항목도 포함하되 새 지출에는 활성 항목만 사용한다. 기존 지출의 비활성 분류를 유지하는 수정은 허용한다.
- 모든 ID/금액은 JSON 문자열이다. 예산 미설정은 `planAmount:null`; 화면은 계산용 0을 배정액으로 표시하지 않는다.
- 금액 계산은 서버가 담당한다. 브라우저는 문자열/BigInt 기반 표시만 수행한다.

오류: `{code,message}`. 400 `INVALID_INPUT`, 401 `UNAUTHENTICATED`, 403 `FORBIDDEN`, 404 `NOT_FOUND`, 409 `CONFLICT`, 500 `INTERNAL_ERROR`. API 인증 실패는 로그인 HTML로 리다이렉트하지 않는다. 기존 HTML 화면의 로그인 리다이렉트 동작은 유지한다.

예산·지출 변경 POST 성공 뒤 GET으로 월 요약을 갱신한다. 카테고리 생성은 201 응답의 DTO를 선택 목록에 즉시 추가하며 GET을 보내지 않는다. GET 실패는 저장 실패로 표시하지 않고 화면 갱신 재시도만 제공한다. POST 결과를 확인할 수 없는 네트워크 장애에서는 자동 재전송하지 않는다. 401/403은 초안을 현재 화면에 남기고 재로그인/새로고침을 안내한다. 카테고리 생성과 지출 저장은 별도 트랜잭션이다.
