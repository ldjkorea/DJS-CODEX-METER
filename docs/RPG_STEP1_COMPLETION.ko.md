# GPT HUD RPG Evolution — 1단계 로컬 완료 보고

검증일: 2026-10-11 / 구현 커밋: `68b9871`

**로컬 구현·검증 완료. 사용자 휴대폰의 실제 복구는 미확인. Push·Tag·Release·공개 APK 업로드 없음. 2단계 미착수.**

## 1. 작업본과 확인된 리셋 문제

- 실제 저장소: `tier-evolution/codex-meter-ko`, origin `ldjkorea/codex-meter-ko`.
- 시작 HEAD: `72455166efbfeb5f7fa7210032ef8a6c9a19d217`, 시작 브랜치 `feature/widget-tier-center`.
- 시작 시 원격 main과 최신 릴리스를 읽기 전용으로 확인: HEAD 일치, 최신 Android 릴리스 `2.8.20-beta` / versionCode 50.
- 독립 로컬 브랜치: `feature/rpg-step1-monthly-tier-protection`.
- 기존 미추적 `docs/emblem-concepts/`는 수정·삭제·커밋하지 않음.
- Android/Wear 버전 `2.8.20 / 50`, 패키지 `dev.bennett.codexmeter`, 서명 설정 유지.

확인한 원인은 **앱의 계산·표시 정책**이다. 기존 `LiveUtilization`은 원인 미확인 조기 리셋/보정 시 이전 소비 segment를 제거했고, `SubscriptionValueUi`는 이 임시 결과로 가성비를 표시했다. `TierTheme`와 `LiveCards`는 실시간 활용률 및 최신성에 따라 티어를 직접 선택했다. 계정·요금제별 기존 티어 저널과 실시간 티어가 서로 달라질 수 있었다.

새 월간 결산과 활동 티어는 이 임시 결과를 권위 있는 기록으로 사용하지 않는다. 기존 실시간 엔진은 현재 한도·AI 문구 등 호환 경로에 유지했다. OAuth·UsageApi·서버 요청 형식·원본 SQLite 스키마를 변경하지 않았다.

**10월 11일 서버 변경의 실제 원인이 Tibo 전역 초기화인지, 쿠폰인지, 서버 보정인지는 확인하지 못했다.** 예정 시각의 정상 rollover, 확인된 credit operation, 정책 변경, 조기 구간 변경, 보정/공백을 구분하며 확인되지 않은 원인은 확정하지 않는다.

## 2. 복구 범위와 원본 보호

복구할 수 있는 근거:

1. 실제 SQLite 관측값의 원본 decimal과 reset/window/plan 정보.
2. 원시 관측이 정리된 날짜의 비 legacy 일별 집계. 원시 관측이 표현하는 동일 구간과 중복 합산하지 않는다.
3. 기존 FunStore의 완결 주간 결산 중 `near_boundary_observations_no_interpolation`, 충분한 관측 수, 완전한 7일 및 전체 결제 기간 귀속이 확인되는 기록. 원장/일별 집계와 겹치면 해당 구간에서 더 큰 확인값만 반영한다.
4. 기존 구독료 수정 이력과 기존 플랜별 성취 저널.

복구 전 `files/rpg-step1-recovery/snapshot-<UUID>/`에 원본 원장 DB와 존재하는 WAL/SHM, 결제·Fun·Tier preference 사본을 저장한다. 파일별 SHA-256 manifest와 완료 marker를 남긴다. 실패한 사본은 지우거나 덮어쓰지 않고 다음 시도는 새 폴더를 사용한다. 인증 preference는 복사하지 않는다. 원본 DAO 잠금과 NETWORK_LOCK으로 정상 앱 기록/계정 전환과 순서를 맞춘다. 이 사본을 원본 앱 데이터에 자동 재주입하지 않는다.

별도 recovery 테이블에도 계정별 첫 복구 이전 journal을 보존한다. 저장 실패 시 트랜잭션을 rollback하고 마지막 정상 결산을 유지한다. 표시 캐시가 없어지거나 손상되면 영속 DB에서 다시 읽는다.

**휴대폰 원장에 저장되지 않았던 소비, 조회 공백 내 소비 시각, 리셋 직전의 미관측 소비는 만들지 않는다.** 원장 보존 기간이 지났고 확인 가능한 일별/완결 결산도 없으면 복구할 수 없다. 이번 검증은 합성 입력이며 사용자의 휴대폰 DB를 열거나 수정하지 않았다.

## 3. 실제 결제 기간의 월간 결산

새 `MonthlyLedger`가 입력한 결제 시작일(포함)~종료일(제외), Asia/Seoul 기준으로 계산한다. UTC 관측 timestamp는 그대로 유지한다. 28·30·31일 등 실제 등록한 기간을 사용하며 달력 월/주간 종료일에 결제 결산을 초기화하지 않는다.

- 서버 구간 시작이 결제 기간 안에 있는 최초 정상 counter는 해당 구간에서 확인된 소비로 사용할 수 있다. 자정/초기화 시점의 가짜 관측을 만들지 않는다.
- 같은 구간의 정상 증가를 원본 decimal `BigDecimal`로 누적한다.
- 정상 주간 초기화는 이전 소비를 남기고 새 구간에서 실제 관측한 counter만 추가한다.
- 조기 리셋·보정·확인된 credit operation은 새 baseline을 잡는다. 이전 확인 소비는 유지하며 이후의 양수 증가만 추가한다. 쿠폰 자체를 100% 소비로 바꾸지 않는다.
- 결제 경계를 가로지르는 delta는 비례 보간하지 않고 미확인 범위로 남긴다. 그 이후 온전히 기간 안에 속하는 증가만 반영한다.
- 조회 공백의 같은 구간 양수 net 증가가 확인되면 최소 소비로 보존하되 특정 날짜에 임의 배분하지 않는다.
- 5시간 소비는 월간 주간 활용도에 섞지 않는다. weekly/monthly/plan/window 정책별 단위는 독립이다.

같은 정책에서 기준 배정량은 `결제 기간 길이 / 서버 한도 구간 길이 × 100`이다. 예를 들어 28일의 7일 한도는 400%p이고, 확인 소비 80%p + 40%p는 활용도 30%다. 이 값은 주간 counter 30%와 다르다. 추가 초기화 후 실제 소비가 계속 확인되면 결제 활용도가 100%를 초과할 수 있다.

서로 다른 플랜의 한도 배정량을 같은 실물량으로 변환할 정보가 없으면 플랜별 관측·소비는 보존하지만 **합산 금액은 표시하지 않는다.** 플랜 변경의 정확한 유효 시각을 관측 사이에서 추정하지 않는다. Pro→Free→Pro는 3개의 관측 구간으로 남긴다.

참고 금액은 `직접 입력한 구독료 × 확인된 활용도 / 100`이다. 실제 API 비용·절약액·이론적 최대 API 가치가 아니다. 입력이 없으면 결제액을 가정하지 않는다.

등록한 결제 종료일이 지나면 이전 기간은 `final`로 보존하고, 새 실제 결제 기간을 등록하면 별도 결산을 시작한다. **서버 사용량 API로 결제 갱신/실제 결제액을 확인할 수 없으므로 새 결제일·결제액을 자동 생성하지 않는다.** 최종/잠정과 완전/부분 관측은 별개다. final도 확인된 최소 활용량일 수 있다.

요금이 바뀌면 revision과 계산 snapshot을 남기고 참고 금액을 재계산한다. 같은 플랜의 겹치는 날짜 수정은 기존 period ID의 별칭/revision으로 처리하여 이중 결산을 만들지 않는다. 날짜 변경 시 소비 귀속도 다시 계산한다. 삭제된 근거로 수정된 기간을 안전하게 재배정할 수 없으면 금액 산출을 보류한다.

### 별도 저장소

`gpt_hud_account_progress.db` v1:

| 테이블 | 내용 |
|---|---|
| profiles | account SHA-256 primary key, 버전이 있는 account document |
| recovery | account primary key, 최초 복구 전 document, captured_at |

document는 schema, 삭제 floor, 중복 방지 관측 mirror, 일별 근거, 확인 credit timestamp, billing revisions/aliases, settlements, 삭제 시 보존하는 recap prefixes, legacy heritage, account tier, future 공간을 가진다.

관측 ID는 meter+UTC timestamp, 완결 구간 ID는 policy+reset, 결제 period ID는 최초 plan+start+end와 날짜 수정 별칭이다. 모든 재처리는 동일 자료로 소비를 다시 증가시키지 않는다. 계산 규칙 버전, 원본 decimal/source, 미확인 구간, 계산 revision을 남긴다. 원본 90일/일별 750일 정리와 별도로 결산·최고 티어는 자동 삭제하지 않는다.

삭제/로그아웃 floor가 증가하면 이미 확인된 결산을 prefix로 보존하고 원시 mirror는 재이관 금지 경계 뒤에서 시작한다. 이전 원장/저널은 다시 합산하지 않는다. 장기 mirror 압축은 이번 단계에서 도입하지 않았으며, 대량 다년 기록의 크기·성능을 검증한 뒤 근거 보존형 압축을 별도로 검토해야 한다.

## 4. 티어 보호 정책

`AccountTier` v1이 활동 티어, 최고 티어, 주간 평가 로그, 장기 점수, 보호 종료일을 계정에 저장한다.

- 리셋·오래된 cache·서버 오류·관측 공백은 강등 근거가 아니다.
- 기존 기록 이관 시 새 Free/저활용 플랜 저널이 이전 플랜의 기록된 활동 성취를 낮추지 못한다. 최고 기록은 기존 평가 이력에서도 승계한다.
- 관측 기반 승급 즉시 2주 보호를 적용한다.
- 기존 `FunInsights.completed`의 충분한 양끝 관측·최대 조회 공백·정책 동일성 검사를 통과한 완전한 7일만 유효 평가다. 조기 리셋 구간은 제외한다.
- 유효 주간 2개 연속 유지 기준 미달은 내부 경고다.
- 3개 연속 저활용과 최근 최대 8개 유효 주간의 장기 점수 하락이 함께 확인될 때 최대 한 단계 강등한다. 장기 판단에는 최소 4개 주간을 요구한다. 최근 4개 주간의 가중치는 2, 그 이전은 1이다.
- 정책 변경/연속성 단절은 저활용 streak를 끊는다. 보호 중/승급 시 streak를 초기화한다.
- 벽시계만 지나서 완료 주간을 생성하지 않는다. 실제 마지막 주간 관측 시각까지 완료된 구간을 사용하며, 최신 정상 조회가 없거나 오류가 있으면 주간 평가를 보류한다.
- 최고 티어는 강등 후에도 남는다.

`TierTheme.tier`가 공통 결과다. 홈 휘장, 기존 Android 위젯, 앱 테마, 기존 TierStore 표시 adapter와 Android LAN의 티어 metadata가 이를 사용한다. 기존 휘장·아이콘·티어 색상 자산은 그대로다. Windows 코드는 수정하지 않았으며 기존 phone-mirror 최신성/연결 조건을 유지한다.

## 5. 요금제 변경과 계정 승계

기존 결제 정보의 `SHA-256(accountId|plan)` 키는 그대로 유지하여 다른 플랜에 결제액이 섞이지 않게 했다. 새 성취와 결산은 `SHA-256(accountId)`로 저장한다. auth token/raw account ID를 게임 document에 보관하지 않는다.

Pro→Free→Pro에서도 account document의 활동/최고 티어, legacy 성취·결산·수정 이력을 유지한다. 현재 배너·가격·한도는 실제 snapshot/등록 플랜을 따른다. 다른 계정은 별도 row다. 비동기 작업은 캡처한 계정 키와 현재 계정/조회 snapshot이 일치할 때만 저장한다.

legacy heritage는 기존 Fun/Tier document를 그대로 보존하는 멱등 이관이다. future는 후속 XP·레벨·대한민국/세계 평가·칭호·토큰 통계가 계정 단위로 붙을 공간이다. 이번 단계에서 없는 기록을 생성하거나 새로운 XP/토큰 수집 엔진을 구현하지 않았다.

## 6. 검증 결과와 보존 범위

| 검증 | 실제 실행 결과 |
|---|---|
| `bash android/run-tests.sh` | 전체 기존 self-test/source guard + 신규 계산 48건 + production account DAO 20건 통과 |
| `python android/tests/verify-rpg-native.py emulator-5556 final` | Android API 35 별도 시험 앱에서 native 계산 48건 + SQLite 저장/재열기/계정 격리 12건 통과 |
| Gradle `:app:compileReleaseJavaWithJavac :wear:compileReleaseJavaWithJavac :app:lintRelease :wear:lintRelease` | 최종 소스 컴파일·lint 성공 |
| 기존 Windows 테스트를 별도 `android/build/rpg-windows/WidgetTests.exe`로 컴파일·실행 | 278 assertions, 112 offscreen renders 통과. 실제 OAuth/API/registry 호출 없음 |
| `git diff --check` | 통과 |

자동 검증에는 주간 80→리셋0→소비40, 조기 원인 미확인 리셋, 정상 rollover 근처 credit, confirmed credit 반복, 보정, 28/30/31일, 결제 경계, 갱신, 금액·날짜 수정, Pro/Free 왕복, 3개 저활용+장기 점수, 보호 기간, 공백, 원장 재연결, 일별 근거 재정리, 중복 이관, 재시작, 동시 쓰기, DB commit 실패, 표시 preference 실패, 계정 변경 작업 경쟁이 포함됐다.

기존 source fixture를 교체하지 않았다. 승인된 정확한 변경만 별도 `rpg-reviewed-patches.json`으로 역적용한 뒤 과거의 변경 금지 검증을 유지한다. 신규 실제 production DAO/순수 계산/native 테스트로 새 경로를 별도 검증했다. 기존 비동기 cache fixture에서는 새 DAO를 대체하므로 이 테스트 자체를 새 DB 검증으로 보고하지 않는다.

lint는 phone 0 unfiltered errors/203 warnings, Wear 0 errors/11 warnings다. 기존 phone baseline이 53 errors/111 warnings를 필터한다. baseline을 변경하거나 경고를 테스트 통과를 위해 지우지 않았다. 새 RPG 파일에 별도 lint 경고는 보고되지 않았다. 기존 unchecked/deprecated 코드 및 Gradle deprecation 경고는 남았다.

이번 수정 중 발견한 JSON 플랫폼 호환성 및 payload context 컴파일 오류는 수정 후 최종 재검증했다. native 테스트는 Android JSON/SQLite에서 직접 실행했다.

검증하지 않은 것: 사용자 삼성 휴대폰의 실제 DB 복구, 실제 OAuth/API 조회, 실제 런처 위젯과 티어 색상 화면, 사용자 계정의 Tailscale 전송, 장기간 백그라운드·Doze, 다년 대량 mirror 성능. 에뮬레이터 시험 APK는 별도 패키지에 설치했으며 기존 GPT HUD/사용자 앱 데이터를 설치·삭제·초기화하지 않았다.

유지한 코드/자산: OAuth·토큰 저장·사용량 API, SQLite 원본 schema, AppPreferences, 서명키/alias, 버전, Pretendard, 챌린저 삼족오 앱 아이콘, 휘장 이미지/색상, 원장/기록·CSV/JSON, 업데이트/릴리스 UI, Windows·작업 현황/Tailscale 연결 로직. `UsageLedgerDatabase`는 안전한 사본 생성 메서드만 추가했다. LAN은 기존 payload의 tier 값을 공통 결과로 바꾸었으며 framing/주소/인증/폴링 주기를 바꾸지 않았다.

## 7. 로컬 커밋 및 금지 작업

- 구현·테스트: `68b9871` — Persist account billing settlements and protect tiers across quota resets.
- 이 문서는 별도 로컬 문서 커밋으로 보존한다.
- Push, Tag, Release, 공개 APK/Drive 업로드, Production 배포 없음.
- 공식 배포용 APK 제작·버전 상승·실제 휴대폰 설치 없음. 테스트를 위한 별도 로컬 QA APK만 사용했다.
- 모델/추론 설정을 이 작업에서 변경하거나 적용 확인했다고 주장하지 않는다.

## 8. 2단계에서 재사용할 인터페이스

| 인터페이스/필드 | 용도 |
|---|---|
| `SubscriptionStore.accountKey(Context)` | 안전한 계정 단위 식별 |
| `SubscriptionStore.planKey(Context, plan)` | 기존 플랜별 결제/저널 호환 |
| `MonthlyLedger.calculate(...)` | 결제 기간/정책별 확인 소비·미확인 범위 |
| `AccountProgress.reconcile(..., evaluateWeeks)` | 멱등 근거 이관·월간 결산·티어 평가 |
| `AccountProgress.importLegacy(...)` | 원본 플랜 저널의 계정 성취 승계 |
| `AccountTier.promote/evaluate` | 활동/최고 티어·보호·유효 주간 평가 |
| `RpgProgressStore.reconcile(..., expectedAccount)` | 계정 경쟁 방지·SQLite 원자적 저장 |
| `RpgProgressStore.monthly/peek/tier` | 화면에서 읽는 결산·계정 성취 |
| `TierTheme.tier(Context)` | 홈/위젯/테마/LAN 표시 공통 티어 |
| document `future`, `heritage`, `billing`, `settlements`, tier `highest` | 향후 평가·XP·성취 연결의 계정별 기반 |

**여기서 1단계를 종료한다. 2단계는 별도 지시 전 진행하지 않는다.**
