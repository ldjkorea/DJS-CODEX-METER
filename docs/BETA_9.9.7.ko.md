# GPT HUD BETA 9.9.7 — 첫 P0 개발 단위 / 로컬 설치 후보

## 범위와 기준

사용자가 후속 개발 프롬프트에 따라 베타9.9.7 개발을 시작하고 정식은 건드리지 말라고 요청했습니다. 후속 예산이 지정되지 않은 경우 첫 P0 단위를 완료하도록 작성된 프롬프트에 따라, 이번 단위는 **성장 원본 DB와 화면 요약의 일치·복구**입니다. 전체 P0~P3를 완료한 버전이라는 뜻은 아닙니다.

- 시작 베타 HEAD: `4d8f2c9dacb2368fd795fd4163d759785ecdba8f` (9.9.6).
- 보호한 정식 HEAD: `afc7d0b9749a42c034e7b7c7305cf8a6cd7fd23a` (3.1.0/code87).
- 새 전용 브랜치 `beta/9.9.7`, phone9.9.7/code990007, package `dev.bennett.codexmeter.trial`.
- 기존 베타 서명 인증서 SHA256 `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`.
- 정식 checkout/main/tag/릴리스, 사용자 미커밋 작업, 기존 베타9.9.6 checkout을 변경하지 않았습니다. 새 키 생성/실물 폰 설치/운영 데이터 변경/push/tag/Release/공개 업로드 없음.

## 재현한 결함과 구현

기존 서명9.9.6이 설치된 소유한 합성 AVD에서 화면용 SharedPreferences 요약만 제거했습니다. 원본 SQLite 성장 문서는 동일한데 `load` 후에도 요약이 복구되지 않아 `CACHE_RECOVERY_REQUIRED` assertion이 실패했습니다. 이 실패를 build/beta997/cache-baseline.log에 보존했습니다.

- **BetaGrowthStore**: worker에서 읽은 DB를 기준으로 요약 누락·손상·오래된 기간·다른 계정 표기를 복구합니다. 요약이 동일하면 저장/방송하지 않아 반복 로딩을 막습니다. 표시 저장 실패는 IOException과 계정별 오류 상태로 알리며, 이미 커밋한 DB를 되돌리거나 초기화하지 않습니다. 계정이 바뀌면 해당 작업을 게시하지 않습니다. 활성 여부는 표시 저장과 독립적으로 DB 행을 조회합니다. 이미 있는 DB를 읽을 수 없으면 원래 성장 보상을 추가 계산하지 않고 기록을 보존합니다.
- **MainActivity**: foreground 및 표시 기간 변경 시 기존 worker에 복구를 요청합니다. 홈 렌더 자체는 캐시만 읽으며 DB/네트워크/전체 성장 재계산을 실행하지 않습니다.
- **GrowthActivity**: 이름·새 성장 요약을 갱신 판단에 포함하여 도감/설정에서 돌아올 때 칭호·이름·수집 토글이 즉시 반영됩니다.
- **BetaHomeUi/BetaSettingsUi/BetaCopy**: 복구할 수 없는 오류에 저장 기록 재확인 동작을 제공하고, 오류를 신규 시작으로 표시하지 않도록 구분합니다. KO/EN 문구를 함께 제공합니다.
- **버전/계약**: phone Gradle/AppConstants/build.sh/run-tests 버전9.9.7, code990007. 이전 베타의 고정3.1.0 User-Agent도 현재 베타9.9.7로 바로잡았습니다. Wear3.0.0은 이번 단위에서 유지합니다.

새 성장 규칙4/namespace usage-check-v4, 명예37/칭호24의 ID·조건·획득 시각, 문구600/팁40, 원본 RPG/이전 보관함은 변경하지 않았습니다. 같은 증거 재시도·캐시 복구·화면 이동·복원으로 XP를 추가하지 않습니다.

최종 검토 중 표시 commit 실패가 `activeWorker`의 활성 판단까지 false로 만드는 문제도 실패 주입으로 재현했습니다(`activation-failure-before.log`). 이를 독립적인 DB 존재 확인으로 수정하고, 표시 저장 실패/DB 읽기 실패에서도 이전 성장 보상이 재개되지 않는 검사를 추가했습니다.

## 실제 실행 결과

1. 기존 실제9.9.6에서 요약 손실 재현: 원본 DB 동일, 요약 미복구 assertion 실패를 확인했습니다.
2. 기존 서명9.9.6→9.9.7 실제 APK 덮어 설치. 소유 AVD `gpthudbeta99`의 명시적 데모 계정만 사용했습니다. 앱 삭제/데이터 초기화 없음.
3. 최종 서명 APK의 **native86개 통과**: 기존19개 상태/원본 레벨·XP·기간별 증거·보상·획득일·장착·보관함/이름, 누락·잘못된 JSON·오래된 요약·계정 불일치, 표시 commit 실패, DB commit 성공 후 표시 commit 실패, worker 재시도, 무변경 로딩 방송 없음, 동일 증거24회 동시 처리, 백업 왕복, 실제 Activity 복귀 시 이름·칭호·토글 반영. '보상을 봤음' seen_total은 화면 조회로 증가할 수 있으며 XP/보상 원장은 동일하게 검사합니다.
4. 최종 APK의 **프로세스 종료·재실행 seed4/verify5개 통과**: 요약을 제거하고 원본을 체크포인트로 저장→실제 force-stop→홈 cold start→계측 테스트가 DB를 읽기 전에 캐시 복구 확인→전체 원본 성장 문서 동일.
5. 실제 rules4/카탈로그/보존 **1112개 통과**, 2027/2028 모든 시작 날짜의 주5일 확인 시 Lv30 **22~28일**. 경제 코드는 변경하지 않았습니다. 누적XP: Lv30=232, Lv40=1082, Lv50=5332, Lv100=40332.
6. 별도 기존 MXP엔진1533개/코치30466개 통과. MXP 계산은 rules4와 별개이며 이관·실계정·실기기 증거로 사용하지 않습니다.
7. 실제 베타 업데이트 파서 **56개 통과**:9.9.0~9.9.6에서 가상9.9.7 후보 선택, 현재 버전 중복 제안 없음, 정식3.1.0/베타 분리 및 기존 신뢰 계약. 실제9.9.7 공개 릴리스가 있다는 뜻은 아닙니다.
8. 최종 phone Release assemble/lint **31초 성공**. 보고 오류0, 경고271, 기존 baseline으로 오류53/경고110 제외. baseline 파일을 수정하지 않았습니다. 계측 javac에는 Android/Kotlin annotation classpath 경고12개가 있으나 컴파일·실행을 통과했습니다.
9. 서명/패키지/버전 확인 및 최종 APK의 Pretendard/라이선스 검사 통과. 금지 폰트 없음, 54 archives/1APK.
10. OAuth·세션·업데이트 설치 신뢰·성장식·코치·티어15개 파일과 manifest/Wear/lint baseline/문구 카탈로그가9.9.6과 동일함을 확인했습니다. 새 코드/테스트/문서를 포함한 Git index1965개 검사에서 알려진 비밀키 패턴 및 추적된 키/런타임 파일이 없음을 확인했습니다. 전체 과거 Git 이력 보안 감사는 아닙니다.

### 실패를 유지한 공통 검사

- `tests/run-release-validation.py --bash <Git Bash> --apk <후보>`: 공통 회귀가 `ParserSelfTest: only complete published releases accepted`에서 실패하여 후속 게이트 실행을 중단했습니다. 수정 전9.9.6 원본 관련 소스를 별도 경로에서 컴파일·실행해 같은 assertion 실패를 확인했습니다. 정식 릴리스용 fixture와 별도 베타 파서의 기존 불일치이며 이번에 parser/fixture를 바꾸거나 테스트를 삭제하지 않았습니다.
- `tests/verify-localization.py`: 정식 보호 파일 검사에서 기존 베타 OAuthClient 차이 때문에 실패했습니다. OAuthClient와 해당 검사 파일은9.9.6과 동일합니다. KO/EN XML 전체 파싱은 별도로 통과했습니다. 전체 localization 검사를 통과했다고 보고하지 않습니다.
- 따라서 이번 결과는 베타 전용 JVM/실제 Android 복구 검사/빌드·lint/최종 APK 검사 통과이며, 공통 release-validation 전체 녹색 상태가 아닙니다. 이 실패를 숨기지 않고 차기 공통 검증 진입점 정비 항목으로 남깁니다.

주요 명령: `python trial/run-beta-checks.py`, `python trial/run-beta997-native.py --expect-cache-gap`(기존9.9.6 재현), `python trial/run-beta997-native.py --class-name Beta997RecoveryNativeTest`, `python trial/run-beta997-native.py --class-name Beta997ColdStartNativeTest --cold-restart`, `./gradlew :app:assembleRelease :app:lintRelease`, `python tests/verify-publication.py <APK>`. 상세 실행 로그는 이 worktree의 android/build/beta997에 보관합니다. 최종 APK는 성장 활성 여부와 표시 저장을 분리한 보완까지 포함하며, 그 뒤 최종 APK에 native86개/cold4·5개를 다시 실행했습니다.

## 미완료·미검증과 다음 작업

- **다음 P0**: OAuth 프로세스 중단/취소 재시도/늦은 콜백/계정 A→B 경쟁, 업데이트 권한 화면 복귀와 다운로드 복구. 이번에 해당 동작을 수정하거나 실계정 로그인 성공을 검증하지 않았습니다.
- **P1~P3**: 다음 목표 추천 개선, 넓은 UI/접근성·KO/EN/글자크기 매트릭스, 진단 현지화, 장기 성능 측정은 후속 단계입니다. 이 전체를9.9.7에서 완료했다고 부르지 않습니다.
- 사용자 실물 폰·실계정·실사용 데이터 복원, TalkBack 음성, 모든 OEM, Wear/Windows 실연결은 미검증입니다. 공개9.9.7 자동 업데이트/익명 다운로드 검증은 배포하지 않아 수행하지 않았습니다.
- 다음 첫 재현: OAuthService/AccountSession에서 취소→새 로그인→이전 콜백 순서를 합성 두 계정으로 점검합니다. 별도 외부 배포는 새 승인이 필요합니다.

## 설치 후보

phone APK SHA256: `f660d14e1ed983af9e67063bf1418e8758e51b1b23147aa5573d35a22139726e`.
동일 베타 앱 위에 설치할 후보입니다. 공개 정식 앱용이 아닙니다. APK-only ZIP과 정확한 로컬 커밋의 소스 ZIP을 함께 보관합니다. 서명키/비밀번호/실제 DB/토큰/가상기기/테스트 로그는 소스 ZIP에서 제외합니다.
