# GPT HUD — Development Phase D 인수인계

## 결과와 작업 기준

Phase D의 제한적 코드 수정, 대상 Android 실행 검증, 전체 기존 self-test, Phone/Wear Release 빌드와 lint를 완료했다. 성능·배터리·접근성의 모든 목표를 실기기에서 입증한 상태는 아니다. 공개 Push·Tag·Release·APK 업로드는 하지 않았다.

- 실제 작업본: `tier-evolution/codex-meter-ko`, 브랜치 `feature/development-phase-d`.
- 승계 기준: Phase C `62c127a` 문서 / `4fa2040` 구현, Phase B `5001e8e`, Phase A/B/C 인수인계. 기존 개발 변경을 덮어쓰거나 과거 checkout으로 되돌리지 않았다.
- 구현·테스트·사용자 안내 커밋: `ca32bd5a202124a4e103914687050c9cc103c26f`.
- 내부 목표 2.8.43~46은 공개 버전이 아니다. Phone/Wear 실제 버전은 **2.8.30 / versionCode 60** 유지.
- 패키지 `dev.bennett.codexmeter`, 서명키, SQLite 스키마·원장 산식, 계정 식별·OAuth, RPG 평가·XP·카탈로그 ID, Provider ID, 위젯 설정, Windows/Tailscale 연결은 변경하지 않았다.
- 기존 미추적 `docs/emblem-concepts/`를 수정·삭제·커밋하지 않았다. 실제 사용자 DB를 읽거나 합성 자료를 사용자 앱에 주입하지 않았다.

## 내부 2.8.43 — 실행·도감 성능

### 확인한 문제와 수정

`RpgCollectionActivity.load()`는 같은 계정·같은 표시 상태로 복귀해도 전체 RPG 저널을 다시 읽었다. 필터·칭호 검색 때 대표 메달 슬롯 3개도 매번 다시 만들었다. RecyclerView가 같은 업적을 다시 bind할 때 메달 Drawable을 재생성했다.

`CollectionPresentation`으로 화면 내부 비교 규칙만 분리했다. 기존 계정별 표시 자료와 대표 메달 설정이 같으면 이미 로딩된 화면을 유지한다. 표시 캐시가 없으면 반드시 영구 저널을 읽으며, 계정 변경 시 비교 결과와 초안을 비운다. 로딩 중 추가 갱신 요청은 마지막 작업 뒤 다시 확인한다. 슬롯은 선택 ID·해당 업적 상태·리소스 설정·티어가 같을 때만 유지한다. 같은 ViewHolder/업적 ID의 Drawable을 재사용한다.

새 DB 캐시나 영구 평가 자료를 만들지 않았다. 이미 비동기였던 저널 조회 구조를 유지했고, 화면 객체를 전역 캐시에 보관하지 않는다. 홈·기록·성장 전체 재작성, DB 조회 산식 변경은 하지 않았다.

### 같은 조건 비교

기존 `emulator-5554`와 격리 QA 패키지 `dev.bennett.codexmeter.rpg3qa`에서 Phase C APK와 Phase D APK를 순차 비교했다. 합성 계정·같은 스냅샷·같은 메달 목록·동일 코드의 측정기를 사용했다. 최종 비교 배치 동안 별도 JVM 회귀·Gradle 빌드를 동시에 돌리지 않았다. 아래 시간은 단일 배치이며 반복 실험·실기기 성능 보장이 아니다.

| 항목 | Phase C | Phase D | 해석 |
|---|---:|---:|---|
| 프로세스 콜드 시작, 3회 중앙값 | 1,570 ms | 1,264 ms | OS 파일 캐시는 유지; 새 설치 콜드 아님 |
| 홈 복귀, 3회 중앙값 | 227 ms | 194 ms | `am start -W` Total/Wait 보고값 |
| Main 진입→UI idle 중앙값 | 999 ms | 774 ms | 비동기 데이터 완료를 모두 기다리는 지표 아님 |
| Records 진입→UI idle 중앙값 | 724 ms | 746 ms | 이번 배치에서는 약간 증가 |
| Growth 진입→UI idle 중앙값 | 731 ms | 743 ms | 이번 배치에서는 약간 증가 |
| 실제 설정 화면 진입→UI idle 중앙값 | 725 ms | 754 ms | 등록 Activity인 MeterSettingsActivity |
| 업적 도감 첫/재진입 중앙값 | 808 ms | 791 ms | 이 화면은 저널 로딩 완료도 확인 |
| 동일 상태 복귀 시 저널 재로딩 | 12/12 | 0/12 | 해당 화면의 WORKER 작업 진입 수 |
| 위 12회 처리 합계 | 790.7458 ms | 307.7367 ms | UI 동기화 대기 포함 |
| 동일 대표 슬롯 전체 재생성 | 12/12 | 0/12 | 실제 자식 View identity 비교 |
| 위 슬롯 갱신 12회 합계 | 193.1988 ms | 104.1475 ms | 이번 배치의 렌더 처리 시간 |
| 메달 스크롤 프레임 p95 | 28.845844 ms / 87프레임 | 19.123524 ms / 88프레임 | Window FrameMetrics TOTAL_DURATION; 여전히 16.7ms 초과 |
| 칭호 검색 6회 합계 | 63.2895 ms | 68.2014 ms | 개선을 입증하지 못함 |
| SQLite 10,000행 조회 3회 중앙값 | 339 ms | 294 ms | 조회 코드 미변경; 이번 변경의 효과로 해석하지 않음 |
| 테스트 종료 프로세스 PSS | 186,418 KiB | 201,663 KiB | 후반 추가 검증·객체 영향도 있음; 메모리 감소 미입증 |

콜드 시작 원시값은 `[1749,1437,1570] → [2055,1264,1199] ms`, 복귀는 `[227,161,401] → [194,179,207] ms`다. 이전 시도에서는 스크롤 p95의 방향이 반대이기도 했다. 확정적인 개선은 불필요한 로딩·재생성 감소이며, 앱 전체가 빨라졌다는 결론이나 개선률은 제시하지 않는다.

SQLite 측정은 Android 메모리 DB에 10,000개 합성 관측을 넣고 실제 `UsageLedgerDatabase.observations()`를 호출한 것이다. 사용자 원장을 정리하거나 삭제하지 않았다. 장시간 반복 진입의 누수·GC·최대 메모리는 아직 별도 측정 필요.

## 내부 2.8.44 — 갱신·배터리 구조

`RefreshScheduler.schedulePeriodic()`는 같은 정책이어도 기존 Job을 취소하고 다시 예약했다. `BackgroundRefreshJobs`에서 서비스·주기/지연·정책 reason이 같은 기존 Job을 유지한다. 정책이 바뀌면 기존 예약 경로로 교체하며 중복 정책 ID만 정리한다. 수동·리셋·재시도 Job은 이 비교 대상에 포함하지 않는다.

실제 Android JobScheduler에서 같은 120분 정책 12회 호출 시 기존 예약 marker 교체는 **12/12 → 0/12**였다. 30분으로 변경하면 교체됨을 확인했다. 로그아웃 시 사용량 Job이 취소되고 저장 스냅샷은 유지됨도 확인했다. 자동 모드 OFF는 원래의 고정 주기 모드 전환 의미를 보존한다.

`TaskStatusJobService`는 시작 시에도 작업현황 ON 및 설치된 해당 위젯 존재를 확인한다. 꺼졌거나 위젯이 없으면 취소하고 PC 요청·렌더 경로에 진입하지 않는다. 기존 15분 최선 노력 주기, 상세 화면 15초 확인, LAN/Tailscale 인증·타임아웃·동시 요청 보호는 그대로다.

최종 합성 테스트 종료 pending Job은 2개 → 1개였다. 그러나 후반 로그아웃 검증으로 예약 상태가 달라졌으므로 배터리 개선 근거로 사용하지 않는다. 실제 네트워크 호출 횟수·전류·배터리 사용량·Doze/재부팅·절전 복귀는 미측정이다. WakeLock·상시 서비스·우회 실행·추가 API는 도입하지 않았다.

## 내부 2.8.45 — 접근성과 렌더링

- 도감 필터 선택 문구의 단일 행·고정 48dp 높이를 해제했다. 최대 3행 줄바꿈과 최소 48dp 터치 영역을 유지한다.
- 메달 카드의 기존 이름·획득/잠금·진행 상태 접근성 설명을 유지하고 자식 텍스트·진행바의 중복 낭독을 제거했다.
- `UsageBurnChartView`에 `performClick()` 및 키보드/접근성 focus 경로를 연결했다. 비터치 클릭은 마지막 실제 관측 시각·값을 설명하며 손가락 스크럽 경로는 유지한다. 합성 마지막 관측 42%를 읽는 실제 View 클릭을 확인했다. 새 사용량을 저장하거나 정밀도를 만들지 않는다.
- 실제 도감 Activity 재생성 후 검색어·카테고리 유지, 표시 캐시가 없을 때 영구 저널 재조회, RecyclerView viewport 재활용을 확인했다.

렌더 검증 범위는 다음과 같이 구별한다.

1. 상세 메달 공통 View: **320/360/411/700dp × 글꼴 1.0/1.3/1.5/2.0 × 한국어/영어 × 라이트/다크** 측정·렌더. 320dp/2.0 한국어 캡처를 직접 확인했다. 이것은 전체 Activity/팝업/시스템 화면의 모든 조합 검사와 다르다.
2. 실제 성장 Activity와 기존 상세 진입: 320/360/411dp × 글꼴 1.0/1.5 × 한국어/영어 회귀 133검사. 성장 320dp/1.5 한국어 캡처를 직접 확인했다. 좁은 화면의 기존 1열 전환을 유지했다.
3. 작업현황 실제 store/RemoteViews: 1~6개 합성 작업·글꼴·언어 회귀 873검사. 320×180dp/1.5배 캡처에서는 한 작업만 표시된다. 수신 6개와 표시 6개를 혼동하지 않는다. 확대 위젯/삼성 호스트의 실제 용량은 다음 QA에서 확인해야 한다.
4. 기존 백업·암호화·Samsung 합성 RemoteViews·표시 캐시 회귀: Phase D APK로 87검사 통과.

TalkBack 실제 사용자 조작·물리 폴더블 접힘·큰 글꼴의 전체 화면 조합·실기기 뒤로 가기 초점·모션 축소 전체 동선은 미완료다. 기존 ON/OFF 안내·계정 분리·모션 설정을 변경하지 않았다.

## 내부 2.8.46 — 구조·lint·문서

위험이 작은 표현 비교와 Job 예약 비교만 각각 두 작은 helper로 분리했다. 대형 파일은 Settings 1,629행, WidgetRenderer 1,115행, Main 1,058행, WidgetConfig 758행, NowBar 837행, AppPreferences 734행, Ui 685행이다. 이 파일들을 줄 수를 이유로 이동하거나 전면 분해하지 않았다. 조회·보안·원장 계산 엔진은 그대로다.

- Phone lint: **기존 248 → 247 warnings**, 신규 노출 error 0. 차트의 ClickableViewAccessibility를 해결했다.
- 기존 baseline으로 **53 errors·111 warnings가 필터링**되는 상태는 그대로다. baseline 확대나 오류 삭제로 통과시키지 않았다. 따라서 전체 lint 오류가 없다는 뜻은 아니다.
- Wear lint: 0 errors·11 warnings, 기존 범위 유지.
- 남은 Phone 경고의 큰 범주는 UnusedResources 172, PluralsCandidate 24, UnusedAttribute 13, SmallSp 9, ApplySharedPref 8이다. SmallSp는 주로 작은 위젯이다. 데이터 저장용 동기 commit을 이유 없이 apply로 바꾸지 않았다. 기존 baseline의 WrongConstant 등은 다음 통합 QA의 분류 대상이다.
- Android README의 과거 2.8.7 설명을 현재 내부 2.8.30 기준으로 고쳤다. 개발 환경·Pretendard·검증 범위·서명·비공개 개발 정책·RPG 백업과 로그인 복원의 차이를 정리했다.
- 루트/Windows 안내의 오래된 다운로드 버전 안내를 기존 2.8.30 설명과 맞췄다. Windows 프로그램 변경·배포는 하지 않았다.
- `docs/RPG_BACKUP_GUIDE.ko.md` 추가. 기존 Release Notes·CHANGELOG 역사 자료는 수정하지 않았다.

## 검증 명령과 결과

환경: 기존 JDK 21, Android SDK 36/37·Build Tools 36.0.0, Python 3.11. 기존 에뮬레이터와 별도 QA 패키지만 사용했다. 실행 로그·캡처는 git 제외된 `android/build/phase-d-*`에 보관한다.

| 명령 | 최종 결과 |
|---|---|
| `bash android/run-tests.sh` | 전체 기존 self-test 통과; parser/OAuth/update/원장/계정/RPG/위젯/Windows 합성 검증 |
| `python android/tests/verify-rpg-growth-native.py emulator-5554 --reuse-owned-fixture --phase-d-baseline` | Phase C 기준 101검사 및 성능 측정 |
| 같은 실행기 `--phase-d` | 수정 코드 113검사 통과 |
| `python android/tests/verify-phase-d-startup.py emulator-5554` | 기존 QA APK 교체·3회 프로세스 콜드/복귀 측정 |
| 같은 native 실행기 `--phase-d-growth` | 133검사 통과 |
| 같은 native 실행기 `--phase-d-tasks` | 873검사 통과 |
| 같은 native 실행기 `--phase-d-platform` | 백업/암호화/위젯 87검사 통과 |
| Gradle `:app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease` | 최종 소스 빌드 성공·기존 baseline 조건 lint 통과 |
| `apksigner verify --print-certs`, `aapt2 dump badging`, SHA-256 | Phone/Wear 패키지·버전·기존 서명 확인 |
| `git diff --check` | 통과 |

테스트 개발 중 잘못된 Activity 대상 때문에 대기한 시도, RecyclerView 호출 인자 오류, 문자열 instrumentation 인자의 boolean 해석 오류를 고쳤다. 해당 실패/비활성 검사의 결과는 최종 통과 근거에서 제외했다. 실제 사용자 앱을 강제 종료하거나 데이터를 지우지 않았다. 기존 HUD source guard는 이번 검토된 정확한 패치만 되돌려 역사 fixture를 검사하며 새 동작은 native 검사로 확인한다. fixture 자체를 재생성하지 않았다.

## 로컬 빌드 결과

파일은 `android/build/phase-d-release/`에 별도 사본·검증 JSON·SHA256SUMS로 보관한다. 공개 업데이트가 아니며 실제 사용자 앱에 설치하지 않았다.

| 파일 | bytes | SHA-256 |
|---|---:|---|
| GPT-HUD-2.8.30-phase-d-local.apk | 27,038,393 | `8e26420b386bf8aaf8e4e0e84bc3d3a7acbd75463193ce5ccb95f6793a29c3ee` |
| GPT-HUD-Wear-2.8.30-phase-d-local.apk | 16,142,785 | `bc35aa66e9ab6f0b9201824842d91c63706a1675c5da152ef25f60da0e7a8e5c` |

공통 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`. 기존 로컬 키와 일치한다. Wear 산출물은 Phase C와 동일 해시다. 실제 휴대폰 설치본 인증서 비교·로그인/API 조회·자동 업데이트 설치는 미검증이다. 개인키·계정 토큰·사용자 DB·원본 세션 로그를 커밋하거나 외부 전송하지 않았다.

## Phase E 통합 QA 준비사항

1. 실기기 Samsung/AOD/Now Bar/Wear·Doze·배터리 절약·재부팅·Wi-Fi/모바일 전환·장시간 연결 복귀를 검증할 것. 로컬 예약 감소를 전력 절감률로 보고하지 말 것.
2. 전체 Activity를 700dp·글꼴 1.3/2.0·회전·TalkBack으로 확장 검증할 것. 현재 상세 View 매트릭스만으로 전체 UI 통과라고 판단하지 말 것.
3. 스크롤 p95가 16.7ms를 넘으며 검색·기록/성장 진입·PSS 개선을 입증하지 못했다. 여러 반복 배치·비동기 데이터 완료 지표·heap/GC·긴 도감 진입 반복을 추가 측정하고 실제 회귀가 재현되면 수정할 것.
4. 320×180dp/큰 글꼴 작업 위젯은 표시 용량이 작다. 5×3 삼성 호스트 실측, 작은 위젯의 SmallSp·터치 영역을 확인할 것. 임의로 fontScale을 무시하지 말 것.
5. Phase B의 공식 토큰 범위 부족에 따른 안전한 강등 보류, Windows 다중 파일 배치의 비원자성, Phase C의 실제 SAF/새 기기 동일 계정 복원 검증은 남아 있다.
6. 공개 버전 변경·원격 최신 HEAD 비교·서명/상위 versionCode 설치·Pretendard publication 검사는 다음 승인된 통합 릴리스에서 다시 수행할 것. 이 code60 로컬본을 자동 업데이트에 올리지 말 것.

**판정:** 제한적 개선과 로컬 회귀·빌드 완료. 공개 배포 없음. 전력 개선·메모리 개선·전체 접근성 매트릭스·삼성/Wear 실기기 검증은 완료 판정에서 제외한다.
