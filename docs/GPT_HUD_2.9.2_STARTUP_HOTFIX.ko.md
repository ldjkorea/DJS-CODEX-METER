# GPT HUD 2.9.2 시작 멈춤 수정 후보

2026-10-13. 공개 기준 `v2.9.1`, origin/main `bff5556`. 사용자 신고는 앱을 열자마자 멈춤이다. 실제 휴대폰이 연결되지 않아 사용자 ANR 로그는 확보하지 못했다. 아래는 같은 시작 경로에서 재현한 코드 결함과 수정·자동 검증 근거이며, 사용자 휴대폰의 정확한 원인 또는 실제 해결 완료를 주장하지 않는다. 공개 Push·Tag·Release는 아직 수행하지 않았다.

## 원인과 최소 수정

`MainActivity → Ui/TierTheme → RpgProgressStore.peek`에서 유효한 표시 캐시가 있어도 먼저 `UsageApi.NETWORK_LOCK`을 기다렸다. 이 잠금은 HTTP 사용량 조회·원장 처리에서 사용한다. 또한 `TierTheme.tier`가 클래스 모니터를 잡고 원장 잠금을 기다리는 동안, 백그라운드 조회/위젯 경로가 원장 잠금에서 티어 색상을 요청하면 반대 순서의 교차 대기가 가능했다.

- 메인 루프의 `peek`는 동일 계정의 표시 캐시만 읽는다. DB 열기·이관·복구·조회 잠금을 기다리지 않는다. 빈/손상 캐시는 미확인으로 반환한다.
- 계정 재확인과 기존 `safeDisplay`의 일회성 수집 전환 보호를 유지한다. 화면 읽기로 저장값을 수정하지 않는다.
- 기존 worker의 원장 복구·이관·계산 경로는 보존한다.
- 티어 색상 캐시는 불변 결과 한 개를 volatile로 게시한다. 원장/네트워크 잠금을 기다리는 동안 별도 테마 모니터를 보유하지 않는다.
- DB 스키마·OAuth·계산식·컬렉션 revision·위젯 Provider·LAN/Tailscale·Windows 소스는 변경하지 않는다. 기존 XP와 수집을 초기화하지 않는다.

## 재현과 검증

소유한 `emulator-5560`의 격리 QA 패키지와 합성 계정만 사용했다. 테스트용 잠금은 제한 시간 후 해제하며 실제 사용량 API·사용자 DB·실제 Codex 세션에는 접근하지 않는다.

- 공개 2.9.1과 같은 코드의 기존 격리 APK: 실제 Android 메인 루프가 **6,001ms** 대기, 스택 `RpgProgressStore.peek → TierTheme.tier`. 통제된 네트워크 잠금 대기 재현 4 checks PASS.
- 최종 2.9.2 격리 APK: **14 checks PASS**, 캐시 읽기 **39ms**, 조회 잠금을 계속 보유한 상태에서 실제 MainActivity 진입 **2,462ms**, 메인 루프 heartbeat 응답과 홈 화면 캡처 확인. 두 실행의 관측값이며 일반 성능 개선률로 해석하지 않는다.
- 빈/손상 캐시 미확인, 이전 수집 캐시 숨김, 화면 읽기의 무쓰기, 백그라운드 색상 처리, 로그아웃 후 계정 노출 차단 확인.
- 기존 격리 업데이트 체인 v28→v30→v50→v51→v2.9.1에 2.9.2/code83을 `install -r`: **12 checks PASS**. 전체 RPG 문서·XP·티어·ON/OFF·대표 메달 순서·칭호·결제·암호 로그인 blob·원장·위젯·PC 페어링 보존.
- 컬렉션 일회성 이전/독립 ON/OFF/백업/실제 Activity/6작업: **31 checks PASS**.
- SQLite/계정/암호화 백업·복원/RemoteViews: **87 checks PASS**.
- `run-release-validation.py`: 전체 JVM·생산 SQLite·RPG·LAN/Windows fixtures 회귀, 현지화, 공개 소스, 출처·폰트 검사의 네 게이트 PASS.
- Phone/Wear Release 빌드·lint PASS. errors 0; Phone warnings 261, Wear warnings 11. 기존 baseline을 확대하지 않았다.
- 최종 Phone APK의 62개 릴리스 기록과 신규 티어 캐시 클래스를 실제 APK에서 확인했다. 최종 APK 2개의 Pretendard·라이선스·금지 폰트 부재 PASS.

실행 명령: `verify-rpg-growth-native.py`의 `--startup-lock --startup-probe`, `--startup-lock`, `--phase-c`, `--collection-restart`, `--upgrade-continuity --continuity=candidate` (모두 `--reuse-owned-fixture`); `run-release-validation.py`; Gradle `assembleRelease/lintRelease`; `apksigner verify`; `aapt2 dump badging`; `verify-publication.py`.

검증 준비 중 instrumentation의 checked JSON 예외와 debug APK의 다른 서명을 발견해 테스트 코드를 수정하고 기존 키로 서명한 격리 Release APK로 검증했다. 버전 고정 guard를 2.9.2/code83/62개 이력으로 일관되게 갱신한 후 전체 회귀를 재실행했다. 초기 빌드의 오래된 릴리스 기록 패키징을 최종 APK 검사에서 발견해 다시 빌드했고, 아래 최종 파일에서 실제 기록·핫픽스 클래스를 재확인했다. 기존 공개 파일을 교체하거나 검사를 완화하지 않았다.

## 최종 로컬 APK

Android/Wear `2.9.2 / versionCode 83`, package `dev.bennett.codexmeter`. Windows `0.1.10` 변경 없음. 최종 전달 폴더 `android/build/release-2.9.2/final/`은 기존 2.9.1 폴더와 독립적이다.

Phone `CodexMeter-2.9.2-ko.apk`: 27,077,457 bytes, SHA-256 `5a8e0c0573ff266aaefe0e6a6affac8c65418ccb0680bf205cdda4f6a47a84ca`.

Wear `CodexMeter-2.9.2-wear.apk`: 16,147,605 bytes, SHA-256 `681227fec308556c400ad372fbb41c3526b13c432148ba30c69c13148262b852`.

기존 2.9.1 공개 APK와 인증서 SHA-256 일치: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`. 사용자 기기의 실제 설치본 인증서 비교는 미검증이다.

## 남은 확인과 인수인계

삼성 갤럭시의 실제 멈춤 로그·업데이트 후 해결·실계정 OAuth/API·Samsung 위젯/AOD/Now Bar·Wear·실제 Windows↔폰 및 외부망 Tailscale·장시간 Doze는 **NOT VERIFIED**. 에뮬레이터의 메인 루프·합성 업데이트 검증을 실제 사용자 해결로 표현하지 않는다.

공개 배포 승인 후 origin/main·최신 Release를 다시 확인하고 2.9.1을 덮어쓰지 않는 `v2.9.2` 릴리스로 전달한다. 게시 후 비로그인 다운로드와 SHA-256, 생산 updater가 2.9.1→2.9.2를 선택하는지 검증한다. 지금 앱을 열 수 없는 사용자는 브라우저로 새 APK를 받아 **기존 앱 위에 업데이트**해야 한다. 앱 삭제·데이터 삭제로 우회하지 않는다.

이 작업의 로컬 커밋은 이 문서를 최초 추가한 커밋이다. 사용자 `docs/emblem-concepts/`는 그대로 보존한다.
