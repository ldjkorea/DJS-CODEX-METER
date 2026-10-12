# GPT HUD 2.9.1 최종 검증

2026-10-13. 사용자의 정식 배포·자동 업데이트 지시에 따라 2.9.0 안정화와 2.9.1 프로필 공유를 한 릴리스로 통합한다. Android/Wear 2.9.1/code82, 기존 package `dev.bennett.codexmeter`, Windows 0.1.10 유지. 실제 게시 완료와 공개 자산 재다운로드의 최종 증거는 Release의 `VERIFICATION.ko.md`, `release-manifest.json` 및 체크섬을 기준으로 확인한다.

## 변경과 보존

성장 → 프로필 공유에서 익명 기본값/선택 닉네임/날짜 숨김으로 티어·레벨·선택한 획득 메달 최대 3개·보유한 장착 칭호를 PNG로 미리 보고 저장·명시적 공유한다. 계정 ID/이메일/토큰/결제/프로젝트/대화/PC 연결정보는 제외한다. 성장 조회 worker의 계정 변경 경합을 차단했다. SQLite 스키마·게임 계산식·OAuth·LAN/Wear 형식·Provider·서명은 유지한다. 기존 수집을 다시 OFF/0으로 만들지 않는다.

## 최종 로컬 검증

- 전체 Android/JVM/생산 SQLite/RPG/LAN 회귀, 현지화, 공개 소스 민감정보 검사 PASS. release-validation/RESULTS.json의 네 게이트 모두 성공.
- Phone/Wear assembleRelease 및 lintRelease PASS. error 0, Phone warning261+hint2, Wear warning11. baseline을 확대하지 않았다.
- 최종 APK 공식 Pretendard·의존성 출처·라이선스·금지 폰트 부재 PASS (52 archives, 2 APK).
- 기존 공개 2.8.51 APK와 최종 Phone/Wear의 인증서 SHA-256 일치: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`.
- Windows 현재 소스 빌드/291 assertions/112 offscreen renders PASS. 공개 0.1.10 ZIP 재사용, SHA-256 `60a3526e9821210b3a3b51e214081b822dd29fc94ef3972992926cc02fb01492`. Windows source tree는 공개 main과 동일하다.
- 격리 에뮬레이터 기존 v28→v30→v50→v51 합성 업데이트 체인의 checkpoint에 2.9.1/code82를 install -r: 12 checks PASS. 로그인 암호 blob/XP/티어/ONOFF/메달 순서/칭호/결제/원장/위젯/페어링 보존.
- 최종 코드 native 프로필 30 checks PASS; 모델14; PNG 10개에 text/EXIF 개인정보 metadata 없음 및 주입 거부.
- 최종 native SQLite/암호화/복원/RemoteViews PhaseC 87 checks PASS. 컬렉션 일회성 이전/독립ONOFF/이전백업/6작업 31 checks PASS.
- 최신 릴리스 선택, 이전 공개 2.8.51→2.9.1 안내 및 equal/downgrade 차단은 생산 parser를 재사용한다. 게시 후 실제 GitHub JSON을 다시 입력해 검증한다.

실행: `run-release-validation.py`, `verify-publication.py` (최종 APK 2개), `verify-rpg-growth-native.py` (candidate/profile/phaseC/collection), `windows-widget/build.ps1 -Test`, Gradle assembleRelease/lintRelease, apksigner verify, aapt2 badging. 원시 로그와 합성 DB는 ignored android/build에 보관하며 공개하지 않는다.

## 검증 중 발견한 문제와 수정

최초 공개 CI에서 회귀 단계가 실패해 Release 게시를 보류했다. 프로필 JVM 검사가 먼저 실행되는 순서인데 이미 생성된 Gradle 클래스에 의존하고 있었다. Gradle 출력이 없는 격리 사본에서 기존 검사의 `RpgEngine/RpgGrowthFacts/RpgTitleCatalog cannot find symbol` 실패를 재현했다. 앞선 RPG JVM 검사의 클래스 경로를 사용하도록 수정하고 cold 테스트 14개를 통과시켰다. 실패 로그의 마지막 60줄이 CI에 보이도록 검사 runner도 보완했다. APK 코드나 데이터 정책은 바꾸지 않았다. 최종 CI run은 release manifest에 기록한다.

## 실제 화면과 제한

프로필 실제 Activity/Bitmap을 320/360/411dp, 한국어 light 320dp 1.5배와 영어 dark 411dp 2배로 렌더했다. README 프로필 이미지는 명시적 합성 Diamond 예시이다. 기존 2.8.51 화면 캡처는 이전 자료임을 표시한다. 모든 화면·700dp·다중 OS·TalkBack 매트릭스를 이번 배포에서 새로 완주한 것은 아니다.

실제 삼성 업데이트·실계정 OAuth/API·홈 위젯/AOD/Now Bar·Wear·사용자 Windows↔폰·외부망 Tailscale·장시간 Doze/배터리/재부팅·카카오톡/Quick Share 수신은 **NOT VERIFIED**. 저장 중 회전/프로세스 종료에서는 재시도가 필요할 수 있다. 자동 확인은 기존 사용자 설정/OS 주기를 따르며 설치 승인 자체는 Android에서 누른다.

## 설치

기존 앱 **기록 → 설정 → 앱 정보 및 라이선스 → 업데이트 → 새 버전 확인**, 다운로드 후 Android **업데이트**. 기존 앱을 삭제하지 않는다. 업데이트 후 로그인·수집 ONOFF·장착 메달/칭호·구독 결산과 성장 → 프로필 공유를 확인한다. Windows 0.1.10 사용자는 재설치할 필요가 없다.

## 소스 이력

안정화 `5a1840d`, 프로필 공유 `037cd0c`, 차기 설계/검증 문서 `e7c7a35`. 정식 버전/릴리스 기록/설치 안내/보고서/버전 검사 갱신 커밋은 v2.9.1 Tag가 가리키는 커밋이다. 사용자 `docs/emblem-concepts/`와 이전 작업본은 보존했다. AI HUD·PROJECT HUD 및 DUEL은 설계/연구 문서만 있다.

## APK SHA-256

Phone `efca096a771efe2c6252ce5088b7bbcdb8aef17d1940ac9ef9a1603fce4e3fe6`

Wear `2f12da2968ce99521a3d814996b9c047131a5b4ce0c5def3fafef588e1f43fa6`
