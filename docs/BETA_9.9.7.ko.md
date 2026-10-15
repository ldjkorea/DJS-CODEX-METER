> 2026-10-15 후속 승인: 사용자가 베타9.9.7 공개 업데이트 배포를 명시했습니다. 아래 구현/검증 보고는 da5533e 시점의 로컬 결과이며 당시 공개하지 않았다는 기록을 보존합니다. 이번 배포는 동일 APK의 해시·서명과 베타 채널을 검증해 beta-v9.9.7 prerelease로 게시합니다. 정식 main/v3.1.0/latest/자산은 보존합니다. 공개 완료·익명 다운로드·실제 업데이트 선택 결과는 설치 자료의 배포 영수증에 기록합니다. 실기기·실계정 미검증과 공통 검사의 기존 불일치는 계속 유효합니다.
# GPT HUD BETA 9.9.7 — 로그인·업데이트 복구와 다음 목표 화면

## 범위와 기준

최신 요청에 따라 성장 저장 복구에 이어 **로그인·업데이트 수명주기 복구와 다음 목표 중심 화면**을 베타 전용으로 구현했습니다. 전체 후속 프롬프트 P0~P3 완료를 뜻하지 않습니다.

- 시작 베타9.9.6: `4d8f2c9dacb2368fd795fd4163d759785ecdba8f`.
- 이번 계속 개발의 기준 HEAD: `d01f0a37010cbf4a54cf4c15944d0d199064bcf0`.
- 전용 worktree `WORKTREES/beta-9.9.7`, branch `beta/9.9.7`.
- phone9.9.7/code990007, package `dev.bennett.codexmeter.trial`.
- 기존 인증서 SHA256 `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`.
- 보호한 정식3.1.0 HEAD `afc7d0b9749a42c034e7b7c7305cf8a6cd7fd23a`, 기존 베타9.9.6 checkout 및 사용자 미커밋 작업을 보존합니다.
- push/tag/Release/공개 업로드/실물 폰 설치/운영 데이터 변경/새 서명키 생성 없음.

## 실제 구현과 변경 이유

| 파일 | 구현과 이유 |
|---|---|
| OAuthAttempt / AccountSession | 로그인 시도별 세대 번호와 원자적 토큰 반영으로 취소된 시도·중복 콜백·다른 계정 선택 뒤의 늦은 토큰 설치를 차단합니다. PKCE 비밀을 저장하지 않습니다. |
| OAuthService | 새 시도가 취소된 이전 worker를 기다리지 않도록 분리하고, 이전 worker가 새 콜백 소켓·서비스·결과를 닫지 못하게 합니다. 브라우저 준비·결과 방송도 시도 번호로 확인합니다. |
| MainActivity / TrialDemo | 로그인 취소 및 재시도, 프로세스가 종료된 뒤 남은 대기 표시 해제, 오래된 READY/RESULT 무시, 데모 선택 후 이전 로그인 무효화를 구현합니다. |
| UpdateActivity | 선택 버전·설치 권한 대기를 화면 재생성에서 복원합니다. 권한 허용 후 이어서 처리하고 취소·중단은 재시도 가능한 상태로 돌립니다. 공유 APK 임시 파일 작업은 worker 잠금으로 직렬화하며 종료한 화면의 worker는 새 오류 상태를 덮어쓰지 못합니다. |
| BetaNextGoal / BetaGrowthModel | 단순 달성률 순서 대신 남은 확인 횟수·이번 기간 달성 가능성·기한을 기준으로 기존 명예 목표를 선택합니다. 새 보상·증거·예상 획득일을 만들지 않습니다. |
| BetaNextGoalUi / BetaGrowthUi | 다음 목표를 레벨 상세보다 먼저 배치하고 휘장·획득 조건·진행 막대·이번 주/달 조건을 표시합니다. 오늘 확인 완료 상태에서는 추가 확인 동작 대신 조건을 제공합니다. 수집 중지는 설정으로 연결합니다. |
| BetaGrowthStore / BetaHomeUi | worker가 게시한 목표 요약만 홈에서 읽습니다. 홈 렌더에서 DB 이관·네트워크·전체 이력을 계산하지 않습니다. |
| BetaCopy | 로그인 중단·업데이트 복구·목표 상태를 한국어/영어로 제공합니다. |
| trial tests / runner | 실제 Activity 재생성·로컬 OAuth 콜백·SQLite·프로세스 종료 및 언어/테마/글자크기 검사를 추가합니다. |

앞선 P0(d01f0a3까지)의 DB 기준 표시 요약 복구·표시 저장 실패 분리·계정별 오류 재확인·이름/칭호/설정 복귀 갱신도 포함합니다. DB 원본이 읽히지 않으면 기록을 보존하고 이전 성장 지급을 재개하지 않습니다.

새 성장 rules4/namespace usage-check-v4, 명예37/칭호24의 안정 ID·획득일·장착·보관함, 문구600/팁40, 기존 티어 색상·자산을 유지합니다. OAuthClient/SecureTokenStore/UpdateInstaller의 인증·암호화·APK 해시/패키지/서명/버전 검증 계약은 변경하지 않습니다. 기존 PKCE·콜백 포트1455/1457·시간 제한·Android 설치 동의 절차도 유지합니다. 새 의존성·AI API·분석 서버 없음.

## 재현한 실패와 보완

- 기존9.9.6의 표시 요약 손실 시 `CACHE_RECOVERY_REQUIRED` 실패를 재현한 뒤 DB를 기준으로 복구하도록 수정했습니다.
- 표시 저장 실패가 활성 여부까지 false로 만드는 결함을 실패 주입으로 재현하고 활성 여부를 DB에서 독립적으로 확인하도록 수정했습니다.
- 이전 APK에서 화면 재생성 후 설치 권한 대기가 사라지는 `UPDATE_PERMISSION_STATE_REQUIRED` 실패를 재현했습니다.
- 업데이트 worker 잠금을 이용한 검사에서 Activity.finish() 직후 onDestroy() 전까지 취소 표시가 늦어 새 오류를 덮어쓰는 실제 경쟁을 발견했습니다. finish()에서 즉시 취소하도록 수정한 최종 APK로 재검사했습니다.
- 초기 UI 검사는 AppCompat 언어 변경이 테스트 시작 전에 반영되지 않아 실제 언어 assertion이 실패했습니다. 테스트를 Android LocaleManager 경로로 준비하고 실제 화면 언어를 검사합니다. assertion을 제거하거나 fixture를 무차별 갱신하지 않았습니다.

## 실행 결과

모든 Android 검사는 소유 AVD `gpthudbeta99` / emulator-5590의 명시적 합성 데모 계정만 사용합니다. 실계정·실사용 DB 검증이 아닙니다.

- 최종 APK 로그인 복구 **18개 통과**: 실제 서비스·브라우저 intent·로컬 콜백·취소/재시도·신규 PKCE 상태·이전 콜백 거절·늦은 토큰/중복 반영 차단. 실제 OAuth 서버 토큰 교환 없음.
- 최종 APK 로그인 프로세스 종료 **seed3/verify4 통과**: 실제 force-stop 후 대기 상태 해제 및 다시 로그인 준비. 기존 합성 자격증명/설정을 복원합니다.
- 최종 APK 업데이트 복구 **18개 통과**: 실제 Activity 재생성·설치 권한 거절/취소/허용 복귀·중단 작업·일회성 intent·종료 worker 차단. 잠금으로 실제 다운로드·설치 직전에 멈추므로 APK 다운로드/설치 성공 증거로 사용하지 않습니다.
- JVM 수명주기/목표 선택 **29개**, rules4/카탈로그/보존 **1112개**, 베타 업데이트 파서 **56개**, 별도 기존 MXP **1533개**, 코치 **30466개 통과**.
- rules4: 2027/2028 모든 시작 날짜의 주5일 확인 시 Lv30 **22~28일**. 누적 XP: Lv30=232, Lv40=1082, Lv50=5332, Lv100=40332. 경제 변경 없음. 별도 MXP 성장 결과를 rules4 결과와 섞지 않습니다.
- 10,000 V4 요약16ms 측정(합성 JVM, DB/네트워크 없음). 실기기 성능 보증이 아닙니다.
- 최종 `:app:assembleRelease :app:lintRelease` **16초 성공**. 보고 오류0/경고271, 기존 baseline 오류53/경고110 제외. baseline 변경 없음.
- Git index1974개 공개 소스 검사에서 알려진 비밀키 패턴 없음/키·런타임 미추적 확인. 전체 과거 이력 보안 감사는 아닙니다. 정식/9.9.6 checkout의 HEAD·clean 상태와 보호한 인증 클라이언트·토큰 저장·업데이트 검증기·성장식·manifest/Wear/lint baseline의 무변경을 확인했습니다.
- 최종 APK 폰트/라이선스/금지 폰트 검사 통과: 54 archives / 1 APK. 계측 javac의 Kotlin annotation classpath 경고38/46개는 남아 있으나 컴파일·실행 성공.

최종 APK의 SQLite/표시 복구 **86개**, 실제 cold start **seed4/verify5 통과**. 기존 원본 레벨·XP·기간 증거·보상·장착·보관함·이름 보존, 표시 commit 실패, 동일 증거24회 동시 처리, 백업 왕복, 실제 Activity 복귀를 재검사했습니다. 보상을 봤음 seen_total은 조회로 증가할 수 있고 XP/보상 원장은 동일하게 검사합니다.

최종 목표 UI는 한국어/영어 × 밝은/어두운 테마 × 글자크기1.0/1.3/1.6/2.0의 세로16조합, 실제640×320 가로 화면의 글자크기2.0 4조합에서 **각30개 검사 통과**. 실제 언어/테마, 목표가 레벨보다 먼저 나옴, 텍스트 생략·세로 잘림 없음, 목표 버튼48dp, 오늘 완료/수집 중지 동작, 조회로 XP 증가 없음, 실제 스크린샷을 검사했습니다. 회전 설정만 바꾼 추가4회는 화면이 세로였으므로 가로 검증에 세지 않았습니다. 전체 화면의 모든 요소·스크롤 끝까지·TalkBack을 검증한 뜻은 아닙니다.

### 기존 공통 검사 한계

앞선 P0에서 `tests/run-release-validation.py` 공통 회귀는 `ParserSelfTest: only complete published releases accepted`에서 실패했습니다. 수정 전9.9.6 소스도 같은 assertion 실패가 재현되었습니다. 정식용 fixture/베타 파서 불일치로 남겨 두며 이번에 parser/fixture/검사를 삭제·변경하지 않았습니다.

`tests/verify-localization.py` 역시 기존 베타 OAuthClient 차이로 정식 보호 검사에서 실패했습니다. OAuthClient/검사 파일은 기존9.9.6과 동일합니다. 이번 KO/EN 실제 화면 검사가 전체 localization 게이트 통과를 대체하지 않습니다. 공통 검사 전체 녹색으로 보고하지 않습니다.

주요 명령: `python trial/run-beta-checks.py`, `python trial/run-beta997-native.py --class-name Beta997AuthRecoveryNativeTest`, `--class-name Beta997AuthProcessNativeTest --auth-restart`, `--class-name Beta997UpdateRecoveryNativeTest`, `--class-name Beta997GoalUiNativeTest`, `--class-name Beta997RecoveryNativeTest`, `--class-name Beta997ColdStartNativeTest --cold-restart`, `./gradlew :app:assembleRelease :app:lintRelease`, `python tests/verify-publication.py <APK>`, `python tests/verify-public-source.py`. 로그/화면은 해당 worktree의 `android/build/beta997/phase2`에 보관합니다.

## 미검증·남은 범위

- 실물 폰, 실계정 OAuth 서버 성공, 실제 네트워크 다운로드/PackageInstaller 설치 완료, OEM별 권한 화면은 미검증입니다. 따라서 사용자 폰의 로그인·업데이트 문제가 해결됐다고 단정하지 않습니다.
- 공개9.9.7을 게시하지 않아 자동 업데이트 수신·익명 다운로드는 미검증입니다. 이번 산출물은 기존 베타와 동일 서명의 로컬 설치 후보입니다.
- 모든 화면의 TalkBack 음성/포커스·장기 성능·진단 전체 현지화·후속 P2/P3 전체는 미완료입니다.
- Wear/Windows/위젯 실제 연결, 실사용 백업 복원은 미검증입니다. 해당 코드·manifest·서명·저장 스키마를 변경하지 않습니다.
- 중단한 다운로드를 바이트 위치부터 재개하는 기능은 구현하지 않습니다. 중단 상태를 복구하고 기존 검증 경로로 재시도합니다.

## 설치 후보

APK SHA256: `d7bc69f396d8400840e316605325c950a6e3f63f555f924705775e98b50d3df3`.
서명된 베타9.9.7/code990007이며 정식 앱과 별도 패키지입니다. APK-only ZIP, 정확한 로컬 커밋의 소스 ZIP과 Git 복구 bundle을 함께 보관합니다. 서명키/비밀번호/DB/토큰/가상기기/테스트 로그는 소스 ZIP에서 제외합니다. 공개 배포 없음.
