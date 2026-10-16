# 9.9.8 베타 공개 배포 승인 및 검증

사용자가 최신 작은 화면 보완본의 베타 배포를 승인했습니다. 제품APK는 ce262e5에서 검증한 SHA256 11b2b8d9330c0e88597858d19326c7231a01ab7c76406fdc9c68151093635f6c을 그대로 사용합니다. 문서·새9.9.7→9.9.8 공개 업그레이드 검사만 추가하며 앱/Wear/정식 버전은 바꾸지 않습니다. 대상은 beta-v9.9.8 prerelease이며 정식latest는3.1.0을 유지합니다.

공개 전 최종JVM 재실행766/29/1112/74/1533/30466 통과. 기존 빌드/lint/11화면조건/서명·폰트·개인키 부재 검증을 유지합니다. 앞선 공통fixture/localization 불일치는 숨기지 않습니다. 실물폰·실계정OAuth·모든OEM 검증은 포함하지 않습니다. 실제 게시/익명다운로드/목록크기/기존파서/합성기기설치 결과는 별도 배포 영수증과 후속 확정 기록으로 구분합니다. 아래 ‘공개하지 않음’은 각 로컬 후보 당시의 기록이며 이 승인으로 베타 공개를 진행합니다.

---

# 최신9.9.8 로컬 후보 — 작은 화면에서 프로필 아래 두 한도 확보

기준360b19b의 후속 검증에서 작은 화면11조건 중4조건(한국어360×780/font1.0·1.3,384×854/font1.3,영어360×780/font1.0)은 두 잔량이 첫 화면 밖으로 밀려 실패했습니다. 이 실패를 삭제하거나 검사를 완화하지 않았습니다. 실기기 USB 연결은0대이므로 아래 결과는 Android35 전용 합성AVD 결과입니다.

## 수정

- BetaHomeUi: 프로필 박스의 티어 휘장·이름·칭호·레벨·XP·등급 기준 진입점은 그대로 유지합니다. 기존 목표/성장 링크2개를 별도 helper로 옮겨 프로필 박스 높이를 줄였습니다.
- BetaExperienceUi: 같은 목표·진행값·획득 수·GrowthActivity 연결을 한도 아래의 다음 행동 카드에 표시합니다. 기능 삭제/새 지급/설정 초기화/새 네트워크·DB 작업 없음.
- 최종 순서: 티어·이름·칭호·레벨 박스 → 남은5시간/주간 한도 → 다음 행동/목표·성장 링크. 모든 프리셋에 적용합니다.

## 이번 APK 실행 결과

- Release assemble/lint18초 성공. 오류0/경고279,기존baseline의53오류/110경고 제외 유지.
- 새 APK 덮어 설치 후 기본420폭/한국어/어두움106개 통과. 추가360×780/384×854/412×915 각각font1.0·1.3·2.0,영어·밝음360×780/font1.0 및780×360/font2.0까지 총11조건1154개 통과. 모든 조건에서 세 프리셋을 확인합니다. 앞서 실패한4조건도 동일한 첫 화면 검사로 통과했습니다.
- font1.0/1.3 세로에서는 두 잔량이 초기 viewport 안에 있음을 확인합니다. font2.0/가로는 스크롤을 허용하고 텍스트 잘림과 순서를 확인하며 모두 첫 화면에 들어간다고 주장하지 않습니다. 성장·목표 링크 보존/기존 성장 상태·합성 인증 정보 불변 포함.
- 기존 경제/인증/설치 신뢰·600개 문구ID/40개 팁 소스 경계 검사와 폰트/라이선스54archives/1APK 검사 통과. 이전 전체JVM/로그인·백업 계측을 새 APK에서 재실행한 것으로 보고하지 않습니다.
- 패키지dev.bennett.codexmeter.trial/9.9.8/code990008,기존 인증서6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3. 새 키 없음.
- APK SHA256: 11b2b8d9330c0e88597858d19326c7231a01ab7c76406fdc9c68151093635f6c.
- 실패 로그small-screen-matrix.log 및small-screen-matrix/,통과 로그compact-profile-build.log/compact-profile-native.log/compact-screen-matrix.log/compact-screen-matrix/는 android/build/beta998 안에 보존합니다. 최종 작은 화면 예시는 compact-screen-matrix/ko-dark-1.0-360x780.png(가상 데이터)입니다.
- 정식3.1.0의HEAD/clean 상태 유지. 공개9.9.7/사용자폰설치/실사용 데이터/원격push·tag·Release·공개업로드 변경 없음. 실물폰/실계정OAuth/공개9.9.8자동업데이트는 미검증입니다.
- 이전 전체 개발본부 정기 백업은 SOURCE=0/DATABASE=0/SESSION=0/SIGNING=0을 확인했습니다. 이번 후보의 개인Drive 보관은 전달 자료에서 별도로 확인합니다.

아래는 이전 후보별 기록입니다. 현재 배치·APK·검증은 위 절을 우선합니다.

---

# 최신9.9.8 로컬 수정 — 프로필 박스 아래에 남은 한도

사용자의 최종 배치 지시에 따라 홈 순서는 모든 프리셋에서 **티어·이름·칭호·레벨 박스 → 남은5시간/주간 한도 → 다음 행동**입니다. 이전의 한도 최상단 배치는 이 절로 대체합니다. 기준824f5da, 버전9.9.8/code990008 유지. 한도 표시/캐시·초기화 처리/기존 프로필 내용은 바꾸지 않고 MainActivity의 카드 순서만 변경했습니다. 기존 후보는 삭제하지 않습니다.

- 새 제품 Release assemble/lint21초 성공: 오류0/경고279,기존baseline의53오류/110경고 제외 유지.
- 새 APK를 소유 합성 Android35 AVD에 덮어 설치하고 세 홈 프리셋의 프로필 첫 박스/그 아래 한도/그 다음 행동,잔량·초기화·캐시·자료 없음·인증/성장 보존100개 검사 통과. 한국어/밝음/font2.0 97개,영어/어두움/font1.0 100개도 통과해 총3조합297개입니다. 모든 조합에서 세 프리셋을 확인했습니다.
- 소스 경계 검사,폰트/라이선스54archives/1APK 통과. 이전 전체 검사/10조합952개를 이번 APK에서 재실행한 것으로 보고하지 않습니다.
- 새 APK SHA256: c6ea97adbd2515dc7eeaf864dae03c38195d25db7fe10d148358ad4d8dfdd03d. 기존 베타 서명/패키지/버전 유지. 정식3.1.0/공개9.9.7 변경 없음. 공개 배포/사용자 폰 설치 없음. 실물 폰/실계정 OAuth/공개 자동업데이트는 미검증.
- 현재 로그: android/build/beta998/profile-first-build.log,profile-first-native.log,profile-first-large.log,profile-first-english.log. 화면 profile-first-preview/ko-dark.png는 가상 자료입니다.

아래는 이전 후보별 기록이며 현재 배치 설명은 위 절을 우선합니다.

---

# 9.9.8 로컬 수정 — 홈 핵심 한도 복구

사용자 지적에 따라 홈 우선순위를 바로잡았습니다. 기준 커밋91ef898. 아래 원래9.9.8 전체 검증 기록은 이전 APK(2990d090…)에 대한 기록이며, 이번 수정 APK의 범위와 검증은 이 절을 우선합니다. 공개되지 않은 같은9.9.8/code990008의 로컬 수정 후보이며 정식3.1.0/공개9.9.7은 그대로입니다.

- BetaQuotaUi: 모든 홈 프리셋에서 맨 위에 5시간 남음/주간 남음을 표시합니다. 남은 비율과 잔량 막대/각 초기화 시간/관측 시각/서버 관측·캐시·자료 부족 상태를 분리합니다. 큰 글씨/좁은 화면은 세로 배치합니다. 한도가 없으면 —이며, 초기화 시각이 지났다고 100%로 바꾸지 않습니다.
- MainActivity: 로그인된 홈의 첫 카드를 두 한도로 고정하고 프로필/티어와 성장 카드는 아래에 유지합니다. 기존 시계 갱신 경로를 재사용하며 홈에 새 네트워크/DB 읽기/성장 지급을 추가하지 않습니다.
- BetaExperienceUi: 간단히 보기의 중복 5시간/주간 텍스트를 제거합니다. 월간 보조 값은 유지합니다. 기존 상세 카드/표시 설정 값은 삭제하지 않습니다.
- Beta998HomeQuotaNativeTest / verify-beta998-source: 세 프리셋의 첫 카드/남음 값/초기화/프로필 유지, 제공 안 됨/스냅샷 없음/확인 실패/오래된 값/초기화 경과/인증·성장 보존을 확인합니다.

## 이번 수정 후보의 실행 결과

- Release assemble/lint19초 성공. 0오류/279경고, 기존baseline의53오류/110경고 제외는 그대로입니다.
- Android35 소유 합성AVD에서 기존9.9.8에 덮어 설치 성공. 한국어/어두움/기본 글자에서97개 검사 통과. KO/EN·밝음/어두움·font1.0/2.0 세로8조합과 font2.0 가로2조합, 총10조합952개 검사 통과. 모든 조합에서 세 가지 홈 프리셋을 확인했고, 큰 글씨/가로에서는 스크롤을 허용합니다. 일반 글자 세로는 두 값이 첫 화면 안에 있음을 확인했습니다.
- Git index1991파일에서 알려진 비밀 패턴/개인키·런타임 파일 추적 없음. 기존 인증/성장식/설치 검증 파일과600개 문구ID/40개 팁 불변 소스 검사 통과. 폰트/라이선스54archives/1APK 검사 통과. 첫 폰트 검사 명령의 ANDROID_HOME 누락 실패 후 올바른 환경으로 재실행해 통과했습니다.
- 실제 새 APK 서명 인증서6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3, 패키지dev.bennett.codexmeter.trial/9.9.8/code990008 확인. 새 키 없음.
- APK SHA256: 2959ae7ef92ad0ab3faa6226111d14d343069d4c655e308cae4cf378e805d147.
- 로그: android/build/beta998/home-limits-build.log, home-limits-native.log, home-limits-matrix.log, home-limits-publication.log. 화면: home-limits-preview/ko-dark.png(가상 자료).
- 이번 수정은 새 홈 한도 회귀를 실행했고, 아래 이전 APK의 전체383/86/16/18개 계측·JVM 전체 검사를 다시 실행한 것으로 보고하지 않습니다. 실물폰/실계정/새 공개 자동업데이트는 미검증입니다. 기존 공통fixture 실패와 이전 APK의 나머지 미검증은 아래 기록을 유지합니다.

---

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
