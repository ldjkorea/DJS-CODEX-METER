# v3.0 AI HUD · PROJECT HUD 타당성

조사일 2026-10-13. 설계만 수행했으며 새 서비스 호출·계정 권한·수집기·백엔드를 추가하지 않았다. 기존 Desktop Token Analytics 문서는 초기 설계의 역사 자료다. 현재 `windows-widget/src/TaskObserve.cs`, TaskTitles, 토큰 수집기와 Android 수신 코드가 실제 기준이다.

## AI HUD

| 기능 | 판정 | 근거와 제한 |
|---|---|---|
| 기존 GPT 한도·초기화·신선도 | 구현 가능 — 데이터 출처 검증됨 | 기존 사용량 응답 파서와 원장에 관측된 값만 표시한다. 공개적인 모든 서비스 공통 한도 API라고 주장하지 않는다. 실제 계정 호출은 이번에 하지 않았다. |
| PC Codex 진행/턴 완료/중단 | 구현 가능 — 데이터 출처 검증됨 | 현재 로컬 이벤트의 `task_started/task_complete/turn_aborted`를 읽는다. 로컬 로그 구조는 변경될 수 있으며 턴 완료는 프로젝트 완성이 아니다. |
| 공식 Codex App Server 이벤트 기반 새 관측기 | 조건부 가능 — 추가 권한 또는 검증 필요 | 공식 이벤트 계약은 확인했다. 이미 실행 중인 데스크톱 세션을 수동 구독하는 별도 클라이언트의 지원 여부는 입증하지 못했다. 사용자 작업을 시작·중단해 시험하지 않는다. |
| Claude 조직 API 사용·비용 | 조건부 가능 — 추가 권한 또는 검증 필요 | 공식 Usage & Cost Admin API는 조직 권한이 필요하고 개인 계정은 대상이 아니다. 이번 앱의 개인 OAuth/한도와 같은 데이터라고 간주하지 않는다. |
| Gemini API 요청별 실제 토큰 | 조건부 가능 — 추가 권한 또는 검증 필요 | 공식 응답 `usageMetadata`가 있다. 사용자가 승인한 자신의 API 클라이언트 응답만 수집 가능하다. 새 유료 생성 요청을 시험 목적으로 만들지 않는다. |
| Claude/Gemini 개인 채팅 구독 잔여 한도 | 현재 불가능 — 신뢰할 수 있는 데이터 부재 | 위 조직/API 토큰 문서는 개인 채팅 앱의 잔여 구독 한도 출처를 입증하지 않는다. 확인하지 못했다는 판정이며 전 세계적으로 API가 없다는 단정이 아니다. |
| 서비스 간 단일 총사용률/공식 사용자 순위 | 현재 불가능 — 신뢰할 수 있는 데이터 부재 | 토큰, 기간별 한도 %p, API 비용은 단위·관측 범위가 다르다. 하나의 실측 합계로 만들 수 없다. |
| 타 서비스 웹 로그인 화면 무단 수집 | 비추천 — 보안·유지보수 비용이 가치보다 큼 | 인증 쿠키/DOM 의존과 계정 노출 위험에 비해 검증 가능한 가치가 작다. |

공식 Codex 문서는 turn/started, turn/completed의 completed/interrupted/failed 및 thread/tokenUsage/updated를 설명한다. 실패 이벤트를 관측하지 않은 상태에서 도구 명령 오류를 전체 턴 실패로 승격하지 않는다. [OpenAI Codex App Server](https://learn.chatgpt.com/docs/app-server).

Claude Usage & Cost Admin API는 조직 API 사용/비용 자료와 관리 권한을 제공한다. 개인 계정의 Claude 앱 구독 정보를 제공한다는 근거는 아니다. [Anthropic Usage and Cost API](https://platform.claude.com/docs/en/manage-claude/usage-cost-api).

Gemini GenerateContentResponse의 usageMetadata는 입력·캐시·출력·생각 토큰을 구분한다. promptTokenCount 안에 캐시가 포함되므로 캐시를 다시 총입력에 더하면 중복이다. [Google GenerateContentResponse/UsageMetadata](https://ai.google.dev/api/generate-content#UsageMetadata).

## PROJECT HUD

| 기능 | 판정 | 최소 기술 검증 |
|---|---|---|
| 사용자가 선택한 로컬 프로젝트 ↔ 세션 연결 | 조건부 가능 — 추가 권한 또는 검증 필요 | 로컬 경로의 별칭/해시를 사용하고 이동·여러 worktree·세션 fork를 합성 데이터로 시험한다. 원본 경로를 휴대폰/공유 카드에 자동 전송하지 않는다. |
| 프로젝트별 관측된 턴 상태/활동 추세 | 조건부 가능 — 추가 권한 또는 검증 필요 | 같은 프로젝트에 여러 세션이 속하는 규칙과 사용자의 연결 선택이 필요하다. 현재 6개 작업 프로토콜은 바꾸지 않는다. |
| 처리 토큰 기록 | 구현 가능 — 데이터 출처 검증됨 | 현재 수집기가 확인한 처리량 범위만 유지한다. 누적 카운터를 차분·중복 제거하고 계정 공식 요약과 합산하지 않는다. 전체 PC 활동/결제 토큰으로 표현하지 않는다. |
| 모든 프로젝트의 객관적인 완료/오류 판정 | 현재 불가능 — 신뢰할 수 있는 데이터 부재 | 턴 완료·명령 실패만으로 제품 완성/전체 프로젝트 실패를 알 수 없다. 확인된 관측 사건과 사용자 확인을 구분해야 한다. |
| 프로젝트 성과/AI 실력 자동 점수 | 비추천 — 보안·유지보수 비용이 가치보다 큼 | 많은 토큰과 긴 작업 시간이 품질/생산성의 증거는 아니다. 원본 코드·대화를 자동 평가로 전송하지 않는다. |

## 최소 분리 제안

현재 parser/store/UI를 통째로 추상화하지 않는다. 새 공급자 출처가 검증된 뒤 읽기 전용 관측 DTO와 공급자별 parser만 먼저 분리할 수 있다. 설계 필드는 provider, source, scope(account/request/local_session), metric, unit, observedAt UTC, serverWindow, precision, confidence, provenanceId, freshness다. 미제공 값은 null/unknown으로 유지하고 서버 한도와 로컬 토큰을 별도 뷰로 표시한다.

중복 키는 출처+관측 범위+요청/세션 ID+counter sequence다. 시간 역행/카운터 감소는 새 경계로 격리하고 확정 소비를 임의로 만들지 않는다. 타 서비스 단위의 정규화나 RPG 보상은 자동 적용하지 않는다. 데이터와 동의를 확보한 뒤 독립 검증을 먼저 수행한다.

인증은 각 공급자의 기존 안전 저장소에서 유지하고 DTO에 토큰을 넣지 않는다. 관측기는 로컬 실행, 기본 OFF, 명시적 사용자 권한, 보관 기간을 갖는다. 새 상시 서버·유료 API·스크래핑·실시간 친구 랭킹은 범위에서 제외한다.

다음 진행 조건: 공식 데이터의 실제 응답 샘플을 사용자의 동의 아래 비밀정보 제거 후 검증, 단위/기간/계정 분리 테스트, 역순/중복/부분 응답과 버전 호환 테스트. 이번에는 실제 개인 계정 데이터에 접근하지 않았다.
