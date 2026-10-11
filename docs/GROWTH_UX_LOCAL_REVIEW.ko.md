# GPT HUD 성장 탭 UX — 로컬 검토 완료

## 상태

- 기준 HEAD: `75be530`. 새 로컬 브랜치: `feature/growth-ux-interactions`.
- 앱 이름 GPT HUD, 버전 2.8.21 / versionCode 51, 패키지 dev.bennett.codexmeter 유지.
- 기존 미추적 `docs/emblem-concepts/`는 수정하거나 커밋에 포함하지 않음.
- 로컬 코드·검증·검토용 APK 제작 완료. Push / Tag / Release / Drive 업로드 / 실제 휴대폰 설치 없음.
- 요청된 모델 선택 상태는 이번 작업에서 변경하거나 확인하지 않았음.

## 구현

1. 대한민국·세계 티어 / RPG 레벨·요금제 인장: 2×2 카드 세트, 동일한 최소 높이와 행별 실제 높이 정렬. 360dp 미만 또는 글꼴 배율 1.25 초과에서는 자연 높이의 세로 배치.
2. 티어 카드 클릭: 현재·최고 등급, 최근 30일/누적 활동, 확인된 연속 활동과 완전 주간, 최근 서울 원장 활동일, 토큰 근거, 활용도 근거, 최근 게임 점수와 다음 휘장, 보호 정책 표시. 국내·세계 기준 설명 구분. 실제 이용자 순위라고 표현하지 않음.
3. 레벨 클릭: 누적 XP·다음 레벨 필요 XP, 실제 award ID의 활동/토큰/활용/업적별 XP 합계, 최근 업적 최초 확인 로그. 저장되지 않은 과거 레벨업 시각을 만들어 표시하지 않음.
4. 인장: 124dp로 확대·중앙 정렬, 차분한 테두리, 클릭 시 Free / Go / Plus / Pro Lite / Pro 인장과 개념 비교. 현재 상품 강조. 실제 결제나 플랜 변경을 실행하지 않음.
5. 기본 성장 화면: 대표 업적 최대 3개만 표시. 획득한 업적만 선택 가능하고 모두 해제도 유지. 전체 도감은 별도 펼침 화면.
6. 칭호: 업적 도감과 독립된 선택 화면. 획득한 업적의 칭호만 장착 가능. 기존 장착·해제·추천과 SQLite 저장 동작 유지. 성장 상단 우측 칩과 홈 휘장 아래에 장착 칭호 노출.
7. 홈: 대표 업적 최대 3개의 작은 심벌. 클릭하면 성장 업적 도감으로 연결. 기존 티어·AI·가성비 구조 보존.
8. 업적·칭호: 기존 41개의 ID·조건·보상·이름을 보존하고 12개 추가해 각 53종. 토큰+활동/주간 조합, 60일 연속 활동, 60/1000 누적 활동일, 24회 주말 활동, 확인된 새벽 소비+활동/주간, 활용도 75/150%, 요금제 3종, 완전 주간 8회 등. 공백을 활동으로 간주하지 않음. 임의의 강등·복귀 이력은 만들지 않음.
9. 업적 심벌: 토큰 결정, 연속 활동 연결고리, 태양, 궤도, 왕관, 새벽 달, 활용도 계기, 기록 깃털 등 경량 Canvas 벡터 문양. 대형 이미지나 새 의존성 없음.
10. 신규 달성: 계정별 첫 확인은 기존 업적을 기준선으로 저장. 이후 새 업적만 한 번 획득 배너 표시, 450ms 밝아지는 효과. 기존 효과 OFF와 시스템 애니메이션 설정 존중. 화면 표시/장착 자체는 XP를 부여하지 않음.
11. 색상: 레벨·업적 진행바는 기존 공통 티어 포인트, 국내/세계 카드와 상세 진행바는 해당 등급 색. 일반 사용량 바의 기존 공통 티어 연결 유지. 소진 위험/가속 사용 경고의 의미 색상 유지.

## 저장·호환성

- SQLite 스키마·게임 계산 규칙 버전·기존 XP 및 티어 임계값 그대로 유지.
- 소형 홈 표시 캐시에 획득 ID 목록만 추가. 기존 캐시가 갱신될 때 획득 심벌이 반영됨.
- 새 업적 정의가 첫 재계산에서 누락되지 않도록 입력 캐시에 도감 revision만 추가. 기존 실제 사실을 재평가하며 award ID별 단조 목표로 중복 보상 방지.
- 대표 선택과 연출 확인 이력은 `codex_rpg_showcase_v1`에 기존 안전한 계정 키로 분리. 인증 토큰을 저장하지 않음. 대표 선택은 이 기기의 표시 설정이며 별도의 PC 동기화 항목으로 확장하지 않음.
- 계정 변경, 로그아웃, 액티비티 종료 때 상세 팝업 보호. 저장 실패는 실패 안내 후 재조회, 기존 성장 원장은 보존.
- 선택한 하위 화면과 스크롤 위치의 재생성 상태 저장 추가.
- OAuth / 사용량 요청 / UsageLedger DB / 위젯 / Windows / Tailscale 코드 변경 없음. 아이콘·폰트·티어 이미지 보존.

## 검증

| 실행 | 결과 |
|---|---|
| `bash android/run-tests.sh` | 기존 전체 self-test와 신규 검증 통과 |
| 신규 `verify-growth-ux.py` | 174개 검증, 한·영 46개 리소스 키/자리표시자 일치, 기존 41종 정의 보존 확인 |
| 기존 RPG 엔진 | 1,184개 검증 통과 |
| 기존 SQLite 계정 DAO | 실제 로컬 SQLite + 가상 계정 34개 검증 통과 |
| 기존 사용량 분석 | 2,181개 검증 통과 |
| Windows 기존 테스트 | 메타데이터 14개, 기존 278개/112회 offscreen 렌더 통과 |
| `verify-rpg-growth-native.py emulator-5554 --reuse-owned-fixture --growth-ux` | 최종 QA APK 628개 검증 통과 |
| `:app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease --offline` | 최종 일반 패키지 빌드 및 lint 통과 |
| 서명·패키지·SHA-256 | 두 APK 서명 검증 성공, 휴대폰 GPT HUD / 2.8.21 / 51 / dev.bennett.codexmeter 확인 |

Android 검증은 별도 패키지 `dev.bennett.codexmeter.rpg3qa`와 가상 계정으로만 수행. 기존 실제 앱 패키지와 데이터에 접근하지 않음. 320/411/700dp, 글꼴 1.0/1.5, 라이트/다크, 한·영, 기록 없음, 카드 실제 클릭으로 팝업 열기, 칭호 실제 버튼 저장, 대표 선택 재조회, 계정 불일치 거절, 반복 연출 방지와 효과 OFF 검사 포함.

휴대폰 lint: 오류 0, 경고 207, 힌트 2. 기존 baseline은 오류 53/경고 111을 필터링하며 변경하지 않음. Wear 오류 0/경고 11. 신규 성장 UI 파일/리소스의 lint 경고는 수정 완료. 테스트 helper의 Kotlin annotation/deprecation 경고와 Gradle 기존 deprecation 안내는 남음.

초기 실행에서 Windows 샌드박스의 프로세스 통신 차단으로 Git Bash/ADB/AAPT 실행이 실패했고, 로컬 검증 실행 권한을 적용해 해결. 새 JVM 검증의 클래스 경로 누락은 수정 후 통과. 과거 소스 fixture나 lint baseline을 재생성해 실패를 숨기지 않음.

실제 삼성 기기, 실제 사용자 DB, 실 로그인/API, 실제 런처 위젯·AOD·Now Bar, TalkBack 실사용, 물리 회전/Doze/장시간 백그라운드는 이번 작업에서 검증하지 않음. 화면 크기·글꼴 변경은 에뮬레이터의 구성별 렌더 검증이며 물리 회전 시험과 구분.

## 로컬 APK

공개된 이전 2.8.21 APK와 별도 폴더에 보관. 검토용이며 버전은 올리지 않았으므로 앱 자동 업데이트 목록에 새 배포로 표시되지 않음. 다음 공개 배포 버전은 검토 후 별도 확정 필요.

- 파일: `GPT-HUD-2.8.21-growth-ux-review.apk`
  - 크기: 26,859,828 bytes
  - SHA-256: `5a239d65777aac71c084070880c5e7821dc06f3614a26c563fcaf26c521d48f0`
- 파일: `GPT-HUD-Wear-2.8.21-growth-ux-review.apk`
  - 크기: 16,092,221 bytes
  - SHA-256: `5c50303cd1df3492ece00650a1ca316a316f645e35997d6a92930955ed74bb9c`

서명 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`. 기존 로컬 공개 2.8.21 APK의 인증서와 실제 비교해 일치. 휴대폰 설치본 인증서는 비교하지 못했음.

검토 파일 경로: `android/build/growth-ux-review/` (로컬 빌드 출력, 공개 소스에 포함하지 않음)

## 변경 파일

- `android/app/src/main/java/dev/bennett/codexmeter/GrowthActivity.java`
- `android/app/src/main/java/dev/bennett/codexmeter/RpgGrowthUi.java`
- `android/app/src/main/java/dev/bennett/codexmeter/RpgDetails.java`
- `android/app/src/main/java/dev/bennett/codexmeter/RpgShowcase.java`
- `android/app/src/main/java/dev/bennett/codexmeter/RpgAchievementMark.java`
- `android/app/src/main/java/dev/bennett/codexmeter/LiveCards.java`
- `android/app/src/main/java/dev/bennett/codexmeter/MainActivity.java`
- `android/shared/src/main/java/dev/bennett/codexmeter/RpgGrowthFacts.java`
- `android/shared/src/main/java/dev/bennett/codexmeter/RpgCatalog.java`
- `android/shared/src/main/java/dev/bennett/codexmeter/RpgEngine.java`
- `android/app/src/main/res/values/growth_strings.xml`
- `android/app/src/main/res/values-ko/growth_strings.xml`
- `android/tests/RpgGrowthUxSelfTest.java`
- `android/tests/verify-growth-ux.py`
- `android/tests/RpgGrowthNativeTest.java`
- `android/tests/verify-rpg-growth-native.py`
- `android/tests/RpgStep2SelfTest.java`
- `android/tests/growth_review.py`
- `android/tests/growth-reviewed-patches.json`
- `android/tests/rpg_review.py`
- `android/run-tests.sh`
- `docs/GROWTH_UX_LOCAL_REVIEW.ko.md`
