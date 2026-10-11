# GPT HUD 업적 도감·칭호 보관함 로컬 검토

## 작업 기준과 배포 경계

- 기준: `0c2dace` / 공개 v2.8.22 / versionCode 52. 시작 시 origin/main과 최신 공개 릴리스가 이 기준임을 읽기 전용으로 확인했다.
- 작업 브랜치: `feature/medal-codex-ux`.
- 기존 미추적 `docs/emblem-concepts/`는 보존하며 이번 커밋에 포함하지 않는다.
- 패키지, 버전, 키, 삼족오 앱 아이콘, Pretendard, 원장 스키마를 변경하지 않았다.
- Push·Tag·Release·공개 APK 변경·자동 업데이트·실제 사용자 DB 쓰기를 수행하지 않았다.

## 기존 불편의 코드상 원인

`RpgGrowthUi`가 카탈로그 전체를 순회해 긴 업적/칭호 카드를 한 번에 생성했다. `GrowthActivity.chooseShowcase()`는 이미지 없는 기본 다중 체크박스 대화상자였고, 칭호 화면에도 업적 설명과 조건이 반복됐다. 문양은 metric 중심의 제한된 도형을 공유했다.

## 구현한 화면

### 업적 도감

- 독립된 화면에서 획득 수, 다음 주요 목표, 대표 메달 세 슬롯을 먼저 보여준다.
- 전체/획득/진행 중/미획득 상태와 토큰/활동/티어/특별 카테고리를 함께 적용한다. 긴 칩 나열 대신 간결한 선택 메뉴를 사용한다.
- 기본은 진행 중이며 해당 목표가 없으면 획득 또는 전체로 안내한다.
- 2열 메달 그리드에서 문양·이름·희귀도·획득 여부·진행률만 표시한다. 큰 글꼴 또는 매우 좁은 화면은 1열로 전환한다.
- 기본 추천/달성 임박/최근 획득/희귀도 정렬은 동일 자료에서 안정적으로 유지된다.
- 상세 대화상자에서 토큰 등 주요 조건, 활동일·주간 등 복합 조건을 각각 충족/미달/확인 부족으로 구분한다. 최초 획득일·보상 XP·연결 칭호도 보여준다.
- 확인할 수 없는 관측을 0으로 표시하지 않는다.

### 대표 메달 편집

- 상단 큰 슬롯 세 개, 아래 획득 메달만 표시한다.
- 한 번 눌러 선택/해제하고 길게 눌러 상세를 확인한다. 네 번째는 허용하지 않는다.
- 1~3개 저장이 가능하며 선택 순서를 유지한다. 취소/뒤로 가기는 기존 설정을 보존한다.
- 기존 자동 추천과 홈·성장 대표 메달 연결은 기존 `RpgShowcase`를 사용한다.

### 칭호 보관함

- 보유 칭호가 기본이며 현재 칭호를 상단에 강조한다. 전체/미획득, 카테고리, 검색, 정렬을 지원한다.
- 작은 문양·이름·희귀도·장착 상태의 간결한 행을 사용한다. 큰 글꼴에서는 상태를 짧은 두 줄로 배치하고 이름과 여백을 확보했다.
- 클릭하면 미리보기, 명시적인 장착 버튼을 누르면 적용한다. 미획득 칭호는 장착할 수 없다.
- 랜덤 추천은 획득한 칭호의 미리보기만 연다. 추천만으로 현재 칭호를 바꾸지 않는다.
- 해제, 계정별 저장, 현재 장착 강조를 유지한다. 보유 칭호가 없거나 해제할 칭호가 없으면 해당 버튼을 비활성화한다.

### 성장과 홈

기존 대한민국/세계·레벨·요금제 카드와 토큰 요약은 유지했다. 성장 기본 화면은 대표 메달 세 개와 현재 칭호만 보여주며 도감/편집/보관함으로 진입한다. 별도 화면에서 돌아오면 기존 성장 화면을 재구성하지 않아 스크롤 맥락을 유지한다.

## 문양과 목록 성능

- 기존 53개 ID를 키로 한 결정적 벡터 조합이다. 네 외곽 프레임, 일곱 중심 문양, 보조 룬/장식 조합을 사용하며 색상만 바꾼 동일 문양은 없다.
- 실제 Canvas 렌더의 알파 형태 해시 53개가 모두 다름을 검증했다. 매번 랜덤 생성하지 않으며 외부 이미지/API/PNG 묶음을 추가하지 않았다.
- RecyclerView·GridLayoutManager·안정 ID·DiffUtil로 재활용한다. 상세는 눌렀을 때만 생성하고 기존 라이브러리를 사용했다.
- 화면 재생성 시 필터/정렬/검색/초안/리스트 위치를 저장한다. 계정 변경 시 이전 상세와 초안을 무효화하며 비동기 결과도 계정을 확인한다.
- 성능은 재활용 구조와 실제 렌더 동작을 확인했다. 실제 폰 FPS나 장시간 성능을 측정한 것은 아니다.

## 데이터 보존 근거

- `RpgCatalog`, `RpgEngine`, `AccountProgress`, `MonthlyLedger`, `RpgTier`, `RpgTokens`, `RpgProgressStore`, `RpgShowcase`, `SubscriptionStore`, `UsageLedgerDatabase`, `UsageApi`, `OAuthClient`, `WidgetRenderer`의 13개 파일이 기준 커밋과 같다.
- 53개 업적 ID/조건/XP 보상과 기존 저장 형식은 그대로다. 표현용 모델을 새로 추가했으며 판정/재계산 엔진을 호출하지 않는다.
- 장착은 기존 두 저장 API만 사용한다. UI 필터·정렬·랜덤 미리보기는 저장값을 변경하지 않는다.
- 가상 QA 계정에서 기존 획득일, 획득 목록, XP, 대한민국/세계 기록이 UI 조작 후 그대로임을 확인했다. 다른 계정 자료 차단, 기존 대표 선택 유지, 앱 화면 재생성·재진입도 검사했다.
- 실제 휴대폰 DB에는 접근하지 않았다. 따라서 사용자 실데이터의 업데이트 후 무손실이나 삼성 실기기 설치를 완료했다고 주장하지 않는다.

## 검증

| 검사 | 결과 |
|---|---|
| `bash android/run-tests.sh` | 전체 기존 self-test와 새 도감 테스트 통과 |
| `python android/tests/verify-medal-collection.py` | 5,523 assertions, 보호 대상 13개 파일 동일, KO/EN 52키 일치 |
| `python android/tests/verify-localization.py` | 기존 현지화·원본 계약 회귀 통과 |
| `python android/tests/verify-rpg-growth-native.py emulator-5556 --medals --reuse-owned-fixture` | 독립 Android 35 QA 에뮬레이터에서 762 checks 통과 |
| 기존 성장 native 회귀 | 614 checks 통과: 성장·홈·요금제·대표 선택·칭호 장착 |
| 휴대폰/Wear Release 및 lint | 두 모듈 성공. 오류 0, 휴대폰 경고 218/힌트 2, Wear 경고 11. 새 도감·문양·리소스에서 lint 오류 없음 |
| `python android/tests/verify-publication.py <phone.apk> <Wear.apk>` | 기존 Pretendard·라이선스·금지 폰트 부재 통과 |
| APK 서명 | 기존 인증서 SHA-256 `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3` |

가상 테스트 자료는 독립 패키지 `dev.bennett.codexmeter.rpg3qa`에만 넣었다. 320/411/700dp, 기본/1.5배 글꼴, 라이트/다크, 한국어/영어 Android View 렌더와 실제 클릭·화면 재생성을 확인했다. 최초 공용 에뮬레이터의 추가 검사는 다른 프로젝트가 전면을 사용해 중단했고 독립 에뮬레이터로 재실행하여 통과했다. 다른 앱이나 사용자 자료를 삭제하지 않았다.

기존 Gradle deprecation, 테스트 컴파일 시 Kotlin annotation 경고, 기존 lint baseline은 별개다. 제거한 긴 UI에서 쓰던 기존 리소스는 보존했으므로 미사용 리소스 경고가 남는다. 테스트를 통과시키려고 업적 조건이나 데이터 검사를 완화하지 않았다.

## 로컬 검토 결과물

- `android/build/medal-codex-review/GPT-HUD-2.8.22-medal-review.apk`
- `android/build/medal-codex-review/GPT-HUD-Wear-2.8.22-medal-review.apk`
- 같은 폴더의 `SHA256SUMS.txt`에 최종 파일 체크섬을 기록한다.
- 화면: `android/build/medal-native/captures/rpg3-fixtures/`.
- 전체 검사 로그: `android/build/medal-all-tests.log`, `medal-final-build.log`, `medal-native-own-run.log`.

이 APK는 같은 버전의 **로컬 검토본**이다. 기존 공개 v2.8.22 파일은 그대로 보존했다. 추후 별도 배포 지시를 받으면 새 versionCode/버전, 설치본 서명, 업데이트 호환성을 다시 확인해야 한다.

최종 SHA-256:

```text
95a642aca1e316bed8b4307502c3b563255c597d403b5fa71cabd5caed4d3765  GPT-HUD-2.8.22-medal-review.apk
1cffe724b4665932ae8090d7323c09a20f04b6fab064de6a37d17c3f64f4fffc  GPT-HUD-Wear-2.8.22-medal-review.apk
```

## 변경 파일

### 제품 코드

- 추가: `RpgCollectionActivity.java`, `RpgMedalUi.java`, `RpgCollectionFacts.java`, `values/medal_strings.xml`, `values-ko/medal_strings.xml`.
- 수정: `GrowthActivity.java`, `RpgGrowthUi.java`, `RpgAchievementMark.java`, `AndroidManifest.xml`의 private 화면 등록 한 줄.

### 테스트와 검토 자료

- 추가: `RpgCollectionSelfTest.java`, `RpgCollectionNativeTest.java`, `verify-medal-collection.py`, 본 보고서.
- 수정: `run-tests.sh`, `RpgGrowthNativeTest.java`, `verify-rpg-growth-native.py`, `verify-localization.py`, `verify-rpg-step3.py`, `rpg_review.py`.
- 기존 UI 위치를 참조하던 검사를 새 상세/보관함으로 옮겼다. 원본 OAuth·원장·위젯·카탈로그 비교는 유지한다.

## 남은 검증과 판정

구현과 로컬 빌드·에뮬레이터 검증은 완료했다. 실제 삼성 갤럭시 업데이트, 실계정 로그인/서버 조회, 홈 화면 위젯의 실기기 표시, TalkBack 사용성, 실제 폴더블 접힘 전환, 시스템 프로세스 종료 후 복구, 장시간 프레임 성능은 별도 검증이 필요하다. 화면 재생성 검사는 프로세스 강제 종료 검사의 대체가 아니다.

공개 배포는 수행하지 않았다. 로컬 검토 가능한 상태이며 배포 준비 시 위 설치 검증과 새 버전 부여가 필요하다.
