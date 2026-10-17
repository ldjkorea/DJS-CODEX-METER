# GPT HUD BETA 9.9.11 — 공개 배포 승인

2026-10-17 사용자가 베타 공개 업데이트를 명시했습니다. beta-v9.9.11/prerelease/latest=false,정식3.1.0 유지. 기존 베타에서 기록→설정→앱 정보→업데이트 확인으로 받습니다. 자동 확인은 기존 설정/주기,설치는 Android 동의를 따릅니다. 게시 완료 여부는 배포 영수증을 확인합니다. 아래는 공개 승인 전 후보 이력입니다.

## 로컬 설치 후보 이력

phone9.9.11/code990011, 기존 베타 패키지·서명 유지. 홈 간소화, 기록 메달 달력, 자동 성장 추천, 개인 관측 기반 일간·주간·월간 메달과 한국어 메달 반응24개를 구현했습니다. **이 항목은 공개 배포 완료를 뜻하지 않습니다.** push/tag/Release를 실행하지 않았으며 공개 베타는9.9.10, 정식은3.1.0입니다. [변경·실행 결과·미검증](docs/BETA_9.9.11.ko.md).

---

# GPT HUD BETA 9.9.10 — 주간 표시 안정화

데모 자동 사용량 작업의 샘플 덮어쓰기와 백그라운드 저장 후 홈 갱신 지연을 수정했습니다. 홈은 티어·한도 통합 카드 → 이번 주간 사용량 → AI 한마디 순서입니다. phone9.9.10/code990010, 기존 베타 패키지/서명/데이터를 유지합니다. [변경과 검증](docs/BETA_9.9.10.ko.md). 베타 prerelease/latest=false이며 정식3.1.0은 유지합니다.

---

# GPT HUD BETA 9.9.9

홈: 티어·한도 통합 카드 → 이번 주간 사용량 → AI 한마디. phone990009, beta-v9.9.9 prerelease. 정식3.1.0/latest 유지. [구현·검증 범위](docs/BETA_9.9.9.ko.md).

# GPT HUD BETA 9.9.8 — 홈·성장 경로·회고·복구 (베타)

- 홈은 티어·이름·칭호·레벨 박스 → 5시간·주간 남은 한도/잔량 막대/초기화 시간 → 성장·주요 행동 순서로 모든 프리셋에서 표시.
- 기존 목표 고정, 성장 경로와 획득 타임라인, 주간/월간 확인 기록 회고와 선택적 이미지 공유.
- 연결/업데이트/수집 복구 센터, 한국어/영어 진단.
- 성장·설정을 선택하는 백업/미리보기·병합·부분 실패 안내, 파일 선택 중 화면 재생성 복구.
- AI 한마디600개 중150개 개정, 안정 ID600/기존 팁40 유지. 앱의 고정 캐릭터로 자동 선택하며 켜기/끄기만 제공. 편집·추가·수동 교체 및 프롬프트 보관함 없음.
- 성장식/원본XP/획득/장착/보관함·기존 서명·정식3.1.0 보존. beta-v9.9.8 prerelease 채널로 제공.

[구현·실제 검증·미검증](docs/BETA_9.9.8.ko.md).

---

# GPT HUD BETA 9.9.7 — 로그인·업데이트 복구와 다음 목표

사용자가 2026-10-15에 베타9.9.7 공개 배포를 승인했습니다. phone9.9.7/code990007, 기존 베타 패키지와 인증서를 유지하며 정식3.1.0은 보존합니다. 이 소스 커밋은 게시 전 검증된 후보입니다. 공개 완료 여부와 익명 다운로드 결과는 배포 영수증에서 확인합니다.

- 로그인 취소·재시도, 이전 콜백/토큰 반영 차단, 프로세스 종료 뒤 대기 상태 복구.
- 업데이트 권한 복귀·화면 재생성·중단 후 재시도와 종료 worker 경쟁 차단.
- 다음 목표를 먼저 표시하고 획득 조건·진행도·이번 기간 조건을 안내.
- 기존 성장 원본·레벨·획득·장착·보관함·티어 색상·문구와 팁 유지.
- 실제 실계정 로그인·물리 기기·네트워크 설치 완료 미검증. 공통 정식 fixture/localization 검사의 기존 베타 불일치가 남아 있습니다.

[상세 변경과 실제 검증 범위](docs/BETA_9.9.7.ko.md).

배포 채널: beta-v9.9.7 prerelease. 정식 latest로 지정하지 않습니다. 공개 시 기존 베타의 설정 → 앱 정보 → 업데이트에서 확인할 수 있습니다. 자동 확인이 켜진 앱은 기존 확인 주기에 발견하며 설치는 Android 사용자 동의가 필요합니다.

---
# GPT HUD BETA 9.9.6 — 홈 티어 복구·수집 목표 확장

- 홈에 기존 티어 휘장과 색상을 복구하고 이름·장착 칭호·레벨·경험치를 한 카드에 배치했습니다.
- 명예37개/칭호24개로 확장했습니다. 확인 일수·주간·월간·레벨의 가까운 목표와 장기 목표를 연결했습니다.
- 기존 레벨·XP·획득 날짜·장착·보관함을 보존하며 추가 XP 없이 확인된 기록에서 새 수집품을 계산합니다. 9.9.5 백업도 안전하게 병합합니다.
- 이름·칭호 변경 후 홈 갱신, KO/EN·큰 글씨·가로 화면을 검수했습니다.

[변경·조건·데이터 보존·검증·설치 설명](docs/BETA_9.9.6.ko.md).


---

# GPT HUD BETA 9.9.5 — 개인화·미션·도감 개편

- 기록·홈·성장 3개 탭을 하나의 작은 하단 내비게이션으로 정리했습니다.
- 성장 화면을 내 이름/장착 칭호 → 레벨 → 다음 목표 → 일일·주간·월간 미션 → 수집 메뉴 순서로 구성했습니다.
- 명예 도감은 기본 2열, 큰 글씨에서는 1열입니다. 잠긴 수집품도 서로 다른 실루엣을 보여주며 정확한 조건·진행도·획득 날짜를 확인할 수 있습니다.
- 안정된 칭호 ID에 이름을 대입합니다. 이름과 한국어/영어가 바뀌어도 획득·장착 기록을 유지합니다. 첫 이름·언어 설정은 건너뛸 수 있고 업데이트마다 반복하지 않습니다.
- 설정을 내 정보 / 화면과 언어 / 연결과 알림 / 앱 정보로 묶었습니다. 기존 세부 기능은 고급 설정의 기존 도구에 남겼습니다.
- 프로필 이미지 미리보기, 대표 메달 선택, 직접 선택한 저장/공유, 계정별 성장 백업의 검증 미리보기·안전한 병합 복원을 제공합니다.

## 새 성장 규칙과 이전 기록

업데이트만으로 성장을 초기화하지 않습니다. **새 성장 시작**을 직접 확인할 때 기존 레벨·XP·업적·칭호·장착을 읽기 전용 보관함에 기록하고 새 성장만 Lv.1/XP0으로 시작합니다. 사용량 원본·계정·이름·언어·기존 설정을 삭제하지 않습니다. 규칙 ID는 앱 버전과 분리되어 한 계정의 반복 실행·다음 업데이트에 다시 초기화하지 않습니다.

| 미션 | 조건 | 추가 경험치 |
|---|---|---:|
| 일일 | 그날 새 사용량 서버 확인 성공 1회 | +1 |
| 주간 | 서로 다른 3일 / 5일 | +20 / 추가 +10 |
| 월간 | 7일 / 10일 / 14일 | +40 / 추가 +40 / 추가 +40 |

주간은 한국 시간 월요일~일요일, 월간은 달력 월입니다. 신선한 성공 응답과 저장 완료만 확인 일수로 인정합니다. 캐시·실패·다른 계정·같은 날 재시도·팁·복사·도감 조회는 보상을 지급하지 않습니다. 대화 본문이나 작업 성과를 판단하지 않습니다. 기간 보상은 별도 계정 SQLite의 안정된 키로 한 번만 지급됩니다. 연간 경험치를 추가하지 않으며 장기 명예/칭호는 유지합니다.

Lv.1~29는 레벨당 8 경험치, Lv.30~39는 40부터 매 레벨 +10, Lv.40~49는 200부터 +50, Lv.50 이후는 700입니다. 누적 Lv.30/40/50/100 기준은 232/1,082/5,332/40,332입니다. 주 5일 확인은 2027·2028년 모든 시작 날짜를 검사해 첫 22~28일에 Lv.30에 도달했습니다. 11개 명예와 5개 칭호를 제공합니다.

로그인 없는 데모는 가상의 첫 30일 기록입니다. 실제 계정이나 API를 사용하지 않으며 실제 성과로 표시하지 않습니다. 기존 AI 한마디 600개와 실전 팁40개 및 로그인 복귀 수정은 유지했습니다. 새 인터페이스·칭호는 KO/EN이며 기존 한국어 농담 카탈로그600개를 영어로 새로 번역한 업데이트는 아닙니다. 새 런타임 AI API·대화 분석·클립보드 감시·분석 서버를 추가하지 않았습니다.

## 검증과 설치

- Release assemble/lint 성공: 0 errors, 272 warnings; 기존 baseline의 53 errors/110 warnings 제외. baseline을 수정하지 않았습니다.
- 새 성장/카탈로그/상태 JVM829개, 기존 성장1533·코치30466개 통과. 정상 앱 전체 회귀 및 한국어 리소스 검사 통과. 실제 베타 파서26·정식 파서13개 통과.
- 전용 합성 에뮬레이터의 실제 View/SQLite/PNG40개, 최초 설정·언어·회전·이름 대화상자13개 통과. 9.9.4→9.9.5 실제 APK 교체와 보존·동시 처리17개 통과. 24회 동시 재처리에서도 보상 한 번만 지급됐습니다.
- 한국어·영어 글자 크기100/130/160/200% 및 가로 화면 10조합에서 실제 화면40개 검사씩 통과했습니다. 본문·버튼·스크롤·내비게이션 겹침은 8조합 각각159개 통과했습니다. 표준 SESL 툴바의 한 줄 말줄임은 본문 잘림 검사와 구분했습니다. 실제 기기/OAuth 성공/사용자 실데이터 이관·복원/모든 OEM 위젯·Wear·Windows의 실제 연결/TalkBack 음성 사용은 미검증입니다.
- 기존 정식 ParserSelfTest fixture는 베타 tag/asset 계약과 다릅니다. 삭제·교체하지 않았습니다. `python android/trial/run-beta-checks.py`가 실제 베타 소스의 계약을 검사합니다. 전체 베타 CI 통과를 주장하지 않습니다.

기존 베타를 삭제하지 말고 설정 → 앱 정보 → 업데이트에서 확인하거나 아래 APK를 덮어 설치하세요. 자동 확인이 켜져 있으면 기존 확인 주기에 발견하며 설치는 Android의 사용자 확인이 필요합니다. 정식 GPT HUD 3.0.0은 별도 앱·채널로 유지합니다.

[9.9.5 APK](https://github.com/ldjkorea/codex-meter-ko/releases/download/beta-v9.9.5/GPT-HUD-BETA-9.9.5.apk) · [APK 한 개만 포함한 ZIP](https://github.com/ldjkorea/codex-meter-ko/releases/download/beta-v9.9.5/GPT-HUD-BETA-9.9.5-APK-only.zip)

패키지 `dev.bennett.codexmeter.trial`, versionCode990005, 기존 인증서 SHA256 `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`.
APK SHA256 `194dcf881fc28f546a8522effb6188271496f9bcd8f195ca20a9ce1e09815b91`. 새 키를 만들지 않았습니다.

---

# GPT HUD BETA 9.9.3

Separate phone-only application: dev.bennett.codexmeter.trial, versionCode 990003.
Regular GPT HUD remains on public main v3.0.0. This beta branch contains the actual generated application sources used for the beta APK.

Updates accept only beta-v9.9.N tags and GPT-HUD-BETA-9.9.N.apk assets from this repository. Normal release assets are ignored. The original Korean signing certificate, package identity, checksum and increasing versionCode are required. Installation requires Android user confirmation.

Growth UI: level ring; day/week/month bars; permanent medals with rarity and locked states; title wardrobe; explicit equip; collapsible collection settings. Growth pull-to-refresh is disabled to avoid unhandled spinners. Rendering does not award XP.

Synthetic demo is explicitly labeled and isolated. Existing collection history, equipment and settings stay in the beta sandbox. No conversation or clipboard monitoring or runtime AI API is added.

Build: set JAVA_HOME and ANDROID_HOME, then run android/gradlew --project-dir android :app:assembleRelease :app:lintRelease. Existing signing material is private and excluded from Git. Without signing material the build is unsigned. Do not create replacement keys to update an installed beta.

Verified locally: normal/beta release selection and trust tests; release build/lint; font/provenance; owned-emulator native UI and signed update continuity. Physical phones and real OAuth accounts are not verified.

9.9.2 fixed legacy internal-permission literals that dropped OAuth READY in the beta MainActivity after onboarding. 9.9.3 additionally forwards the account entry flag through unseeded first-run onboarding, persists the account step across recreation and recognizes the beta return scheme. A local user-opened diagnostics dialog reports installed version and update/install errors without OAuth URLs, tokens or account IDs. Tests exercise actual welcome buttons without a demo, real localhost/receiver behavior, and Android PackageInstaller confirmation. Real account authorization and physical phones remain unverified.
