# GPT HUD BETA 9.9.8 — 개발 정본

기준 HEAD8daf3f76c730759e860709146705423c933d6224. 정식3.1.0/공개9.9.7/사용자 작업은 보존하고 beta/9.9.8에서 개발한다. 공개 배포는 이번 개발 승인에 포함하지 않는다.

## 확정 범위

1. 업데이트 목록/캐시 한도와 검증 진입점 보완.
2. 홈의 상태·한 가지 행동·표시 프리셋.
3. 목표 고정·성장 경로·획득 타임라인.
4. 주간/월간 확인 기록 회고(관측 범위 표시, 추가 XP 없음).
5. 연결/업데이트/수집 복구 센터.
6. 기존 성장 백업과 설정의 선택적 묶음, 미리보기/부분 실패 구분, 파일 선택 중 재생성 복구.
7. AI 한마디 자연스러운 한국식 매운맛 개정. 사용자의 답변에 따라 편집·추가·수동 교체 모두 제외. 끄기/기존 숨김 기록 존중. 프롬프트 보관함은 구현하지 않는다. 연결된 기존40개 팁의 명시적 복사는 유지한다.

성장식·원본 증거·XP·ID·획득일·장착·보관함을 보존한다. 새 AI API/대화 본문 수집/클립보드 감시/분석 서버 없음. 기존 Wear/Windows/위젯/로그인 신뢰 계약은 유지한다.

## 검증 계획

순수 계산/문구 정책/복원/기간 경계 → Release 빌드/lint/폰트·비밀 검사 → 소유 AVD의9.9.7→9.9.8 교체·보존/새 화면/언어·큰 글씨/로그인·업데이트 복구. 실제로 실행한 결과와 미검증을 아래에 기록한다. 기존 공통 정식 fixture 불일치를 검사 삭제로 숨기지 않는다.

## 실제 구현

| 파일/영역 | 변경 이유 |
|---|---|
| BetaExperience / BetaPreferences | 기존 원장을 읽는 목표 고정·회고·타임라인. 별도 계정별 표시 설정으로 원본 성장 문서에 새 필드를 넣지 않음 |
| BetaExperienceUi / BetaHubActivity / MainActivity / FunHome | 티어 정체성 유지, 상태와 주요 행동,3개 표시 프리셋,성장 경로/회고/복구 진입. 홈에는 캐시만 사용 |
| BetaGrowthStore / BetaGrowthUi / BetaNextGoalUi | worker가 게시하는 고정 목표와 성장 화면 연결. 달성/알 수 없는 목표는 기존 자동 추천으로 복귀 |
| BetaProfileActivity / BetaRecapShare | 획득한 Lv30/Lv50 프레임만 제공,실제 PNG 반영,명시적 회고 이미지 공유. 공유 선택창 진입을 외부 전송 성공으로 부르지 않음 |
| BetaBundle / BetaBackupActivity | 성장+기존 이전 형식의 앱/알림/Now Bar 설정+이름/표시 프리셋/대표 메달 선택을 선택해 저장. 로그인/대화/사용량 전체 이력 제외. 기존 성장JSON 호환,계정/형식 확인,복원 미리보기,부분 실패 구분 |
| BetaBackupActivity | 대기 파일을 private cache에 먼저 저장하고 account/file 이름을 saved state로 유지. 실제 화면 재생성 후 원래 파일의 쓰기까지 확인 |
| CoachPolicy / CoachUi / SpicyQuotes / coach-ko-v2.json | 고정 매운맛 캐릭터/자동 선택/켜기·끄기. 개인 편집·추가·수동 교체·관심사/말투 선택 UI 제외. 기존 숨김 ID는 유지.15개 상황150개 문구 개정,450개 유지.600개 안정 ID와40개 팁/연결은 동일. 프롬프트 보관함 없음 |
| BetaDiagnostics | 기존 로컬 진단의 KO/EN 표시. 토큰·인증 URL·대화 본문을 수집하지 않음 |
| ReleaseUpdateClient / UpdatePreferences | HTTP 응답/캐시 한도를512KiB→2MiB로 확장하되 bounded read를 유지. 베타 tag/정확한APK URL/패키지/서명/체크섬/증가 code 계약 유지 |
| beta-verify.yml / trial tests | 정식 fixture와 분리된 베타 실행 경로 추가. 기존 정식 workflow/fixture 삭제·무차별 갱신 없음. 원격 CI는 실행하지 않음 |

주간/월간 회고는 기존 지급 시각으로 XP를 합산하고 확인 일수/획득을 표시합니다. 관측 사용량은 기존 LedgerPresentation을 worker에서 사용하며 자료 부족/읽을 수 없는 창을0으로 표시하지 않습니다. 과거 전체 대화/질문 수/업무 성과를 추정하지 않습니다. 회고는 이번 주/이번 달 기준이며 기존 캘린더에서 다른 기간을 탐색합니다.

묶음 복원은 여러 저장소 전체에 대한 원자적 트랜잭션이라고 주장하지 않습니다. 성장과 설정의 결과를 구분하고 설정 commit 실패 시 일부 설정 반영 가능성을 알립니다. 일반 백업은 로그인 토큰을 포함하지 않으며 기존 고급 인증 이전 경로는 변경하지 않습니다. 새 백업의 숨김 문구 이력 이전/사용량 원장 전체 이전/OS 자동 복원은 포함하지 않습니다. 기존 설치의 숨김 기록은 건드리지 않습니다.

## 최종 실행 결과

- 순수 JVM: 새766개,기존 수명주기/목표29개,성장/보존1112개,베타 업데이트74개,별도 기존MXP1533개,코치30466개 통과. 모든2027/2028 시작일 주5일 확인은 rules4 Lv30=22~28일. 기존 성장식과 레벨 이관/원본 증거는 변경하지 않음.
- 최종 제품 APK Release assemble/lint17초 성공. 보고 오류0/경고279,기존 baseline 오류53/경고110 제외. baseline 변경 없음. 경고0으로 보고하지 않음.
- 소유 Android35 AVD gpthudbeta99/emulator-5590의 새 합성 저장 공간에서 공개9.9.7 seed4개→기존 서명9.9.8 덮어 설치→최종383개 통과. 앱 UID/암호화 합성 자격증명/레벨/XP/증거/획득/장착/보관함/이름/끄기 설정 보존,고정 목표/실제 화면/실제 PNG 프레임·공유 선택창/실제 백업 Activity 재생성 후 파일 쓰기 확인.
- 최종 APK 저장 회귀86개 통과: 표시 저장 실패,SQLite 상태 보존,동일 증거24회 동시 처리,백업 복원,화면 복귀. 기존 검사의 기본9.9.7 조건은 유지하고 expected_version/checkpoint 인자만 추가해9.9.8 원본 fixture를 사용.
- 최종 APK 백업/목록16개 통과: 실제 이름·프리셋·켜기 설정 복원,반복 복원 idempotency,실제 preference commit 실패의 부분 실패 결과,선택적 section/인증 주입 거절,600KB 목록 읽기/캐시 성공과2MiB 한도 확인.
- 최종 APK 로그인18개,로그인 강제 종료3+4개,성장 강제 종료4+5개 통과. 실제 서비스/PKCE 상태/loopback 거절/계정 교체 fence를 사용하되 실계정 토큰 교환 없이 합성 계정만 사용.
- 최종 새 본문4화면(복구/성장 경로/회고/백업)의 KO/EN·밝음/어두움·font1.0/1.3/1.6/2.0 세로16조합+640×420/font2.0 가로4조합, 총20조합에서 각368개/합7360개 통과.
- 최종 APK 폰트/라이선스54archives/1APK 검사,기존 인증서6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3 서명/패키지 dev.bennett.codexmeter.trial/9.9.8/code990008 확인. 새 서명키 생성 없음.

### 실패와 보완

초기 컴파일의 괄호 오류를 수정했습니다. 새 화면 제목은 SESL 고정 높이에서 잘려, 새 화면만 제목을 가변 높이 본문으로 이동해 검사를 통과했습니다. 백업 계측 코드의 Files.readString은 Android에 없어 readAllBytes로 수정하고 실제 재생성 후 쓰기를 재검사했습니다. 사용량 창을 읽을 수 없을 때 회고 안내가 빠지는 것을 발견해 자료 부족 안내를 명시했습니다. 실패 로그는 android/build/beta998에 보존했습니다.

`tests/run-release-validation.py`는 기존 `ParserSelfTest: only complete published releases accepted`에서,`tests/verify-localization.py`는 기존 베타 OAuthClient 보호 파일 불일치에서 실패했습니다. 같은 실패는9.9.6/9.9.7에서도 확인한 이력이며 이번에도 실패를 숨기지 않습니다. 베타 전용 통과가 공통 정식 검사 전체 녹색/원격CI 통과를 뜻하지 않습니다.

### 명령과 증거

`python android/trial/run-beta-checks.py`, `python android/trial/verify-beta998-source.py`, `bash android/gradlew --project-dir android :app:assembleRelease :app:lintRelease`, `python android/trial/run-beta997-native.py --class-name Beta998NativeTest --phase seed`(실제9.9.7), `--class-name Beta998NativeTest`(교체 후), `--class-name Beta997RecoveryNativeTest --expected-version 9.9.8 --checkpoint beta998_qa`, `--class-name Beta998BundleNativeTest`, `--class-name Beta997AuthRecoveryNativeTest`, `--class-name Beta997AuthProcessNativeTest --auth-restart`, `--class-name Beta997ColdStartNativeTest --cold-restart`, `--class-name Beta998UiNativeTest`, `python android/tests/verify-publication.py <APK>`.

최종 로그: android/build/beta998/build-complete.log,complete-native.log,complete-recovery.log,complete-bundle.log,complete-auth.log,complete-auth-process.log,complete-growth-cold.log,complete-ui.log,complete-matrix.log. 순수 테스트는final-contracts.log. 기존 오류는common-validation.log/common-localization.log. 임시 로그/AVD/키는 소스 ZIP에 포함하지 않음.

## 미검증과 보존

실물 폰/실계정 OAuth 서버 성공/여러 OEM/Android26·36 실제 실행/TalkBack 음성/장기 성능/Wear·Windows·위젯 실제 연결은 미검증입니다.9.9.8은 공개하지 않아 앱 내 실제 공개 다운로드·PackageInstaller 경로는 아직 미검증이며 이번 로컬 교체는adb install -r입니다.9.9.7의 이전 공개 설치 증거로9.9.8 공개 설치를 대체하지 않습니다. 홈 렌더에 새 네트워크/DB 이관을 넣지 않았고,기존 인증/서명/설치 신뢰/성장식 파일은 기준과 동일합니다. 정식3.1.0과 공개9.9.7,기존 사용자 작업본/서명/복구 자료는 보존합니다. 새 AI API/분석 서버/대화 분석/클립보드 감시 없음.

현재 공개9.9.7 등 이전 클라이언트는 여전히512KiB 제한이 있으므로 차기 공개 시 해당 기존 앱으로 발견·설치와 공개 목록 크기를 별도로 검증해야 합니다. 사용자 폰 설치·실사용 데이터 변경·push/tag/Release/공개 업로드는 실행하지 않았습니다.

최종 로컬 APK SHA256: `2990d090aa080896e429392a742fe3a8e9511de949efd4eeb09e32dbd21cd704`.

최종 Git index 1989파일 검사: 알려진 비밀 패턴 없음, 개인키/런타임 파일 추적 없음(전체 Git 이력 보안 감사를 의미하지 않음). 정식 작업점 afc7d0b 및 기존 베타 작업점 8daf3f7의 HEAD/clean 상태를 종료 시 다시 확인함.
