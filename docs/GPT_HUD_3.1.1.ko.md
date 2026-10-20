# GPT HUD3.1.1 정식 전환 정본

사용자가 베타9.9.11 확인을 마쳤으며 정식3.1.1로 전환·업데이트 검증·공개 배포하는 제안을 승인했다. 기준 베타5a2d7b77c23683435f45c1d4f13eb0b35e4f4fe9,기존 정식afc7d0b9749a42c034e7b7c7305cf8a6cd7fd23a. 이번에는phone3.1.1/code88/dev.bennett.codexmeter를 기존 키로 서명해정식 latest에 게시한다. Wear/Windows는정식3.1.0,베타9.9.11은별도유지한다. 요청했던0.3.9는기존3.1.0의버전명비교상새업데이트가되지않으므로사용자가동의한3.1.1을쓴다.

## 변경 순서/이유

1. 독립 WORKTREES/stable-3.1.1에서 검토한 베타코드와 기존정식이력을 함께 보존한다. 정식전용테스트/문서·Windows·Wear코드는 기존정식에서 보존한다. 중단된9910전환본과 사용자미커밋원본은 수정하지 않는다.
2. 정식패키지/기존서명/내부permission/원래MainActivity런처/codexmeter OAuth링크를복구하고,베타데모런처는외부진입을제거해정식에서는Main으로보낸다. TrialDemo.prepare의정식거부는유지한다. 베타에서꺼졌던Wear listener를기존정식처럼활성화한다.
3. 정식업데이트파서/채널/인증서계약은기존3.1.0소스를사용한다.88>87과3.1.1>3.1.0을검증한다. UI/백업/프로필이미지의BETA표기와버전/기본변경내역을정식으로맞춘다. 기존이름저장완료표시계약을유지한다.
4. 성장·메달DB/백업형식/엔진/원본XP/업적·칭호/관측원장/로그인복구/팁선택/티어/자동사용량안전코드는베타9.9.11과동일하게보존한다. 기존정식의각저장소이름·계정키를그대로사용한다. 베타앱데이터를정식에자동복사하지않는다.
5. 독립신규합성AVD에서공개3.1.0→3.1.1의설치/기록보존·실제UI·새메달·계정경계·로그인복구를검증한다. 공개후에는구앱의발견/익명다운로드/Android Update동의설치도별도로확인한다.

Windows는latest릴리스의자산파일명에서버전을읽는다. 새릴리스에기존 `CodexMeter-WindowsWidget-3.1.0.zip`과 `CodexMeter-Wear-3.1.0-ko.apk`를동일바이트/원래이름으로함께제공해기존다운로드·업데이트경로를유지한다. 새버전으로위장하거나기존자산을교체하지않는다. 베타에서제외했던`:wear`빌드모듈을정식설정대로복구했고,Windows CI도전체Git이력에서보존계약을검증한다.

정식앱의기존계정변경/로그아웃은공유사용량원장을지우는정책이있다. 이정책은이번에변경하지않는다. 따라서이동작을직접실행하는계정경계스트레스검사는업데이트보존시험과별도AVD에서수행한다. 업데이트보존시험의기존관측행비교를삭제하거나완화하지않는다.

## 기능 범위

홈티어·이름·칭호·레벨/남은5시간·주간한도→이번주간사용량→AI한마디,기록메달달력,성장자동추천과메달/수집품진입. 일간은개인의최근관측기준(같은정책최소7일),주간3/4/5일·월간7/10/14일은양의사용량변화가관측된날짜수로기록한다. 명예3·칭호2추가,한국어624문구/기존실전팁40. XP/레벨/티어식변경이나팁조회·복사보상은없다. 대화본문·클립보드감시·추가런타임AI API없음. 관측은전체작업량/성과/Plus·Pro절대작업량동등성을증명하지않는다.

## 검증 구분과 기존 기준 차이

- 소스계약:베타의경제·메달·보관함·로그인·사용량/원장·코치·티어코드가동일한지검사. 정식의파서/서명/기존컴포넌트·Windows·Wear·정식전용테스트를보존하는지검사.
- JVM:신규정식업데이트20개와기존정상파서19개,메달415/주간21/경계766/목표29/보존1112/MXP1533/코치30466을실행한다. 기존회귀fixture삭제·일괄갱신없음.
- 공통스크립트는파서/업데이터/인증등기존Java검사를통과한후 verify-insights의옛3.0.0/code85고정소스가드에서실패한다. localization은기존베타의OAuthClient개편과옛보호기준차이로실패한다. 별도validation은이두정확한불일치만명시적으로보고하고,원본fixture를보존하며새로운정식소스/업데이트/엔진계약을필수통과로검증한다. 모든공통검사가통과했다고말하지않는다.
- GitHub CI는위구분을동일하게출력하고폰/Wearunsigned컴파일·lint·기존Windows단위빌드를실행한다. 키/Release쓰기권한을추가하지않는다.
- 실제SQLite/에뮬레이터·실물폰·실계정OAuth·Wear/Windows실통신을구분한다. 후자는이번에미검증이며사용자기기에설치하지않는다.
- 새키/앱삭제/초기화/사용자원본이동·삭제/실계정도구변경/Drive자동백업·업로드없음. 최종소스HEAD/APK해시/배포와설치실행결과는 SOURCE/delivery/stable-3.1.1/release-20261017의VERIFICATION.ko.md/release-receipt.json에실제완료후기록한다.

## 2026-10-21 검사 보완 — 앱 바이너리 변경 없음

기준 HEAD는 `6638a50b3771d717bad9c6704f6c9924bda418fa`. 위 2026-10-17의 두 검사 불일치 기록은 당시 결과이며 지우지 않는다. 이번에는 테스트/검증 절차/설명만 수정한다. 앱 Java·리소스·Gradle·서명·사용자 데이터·버전은 변경하지 않으므로 새 APK/태그/공개 릴리스가 필요하지 않다. 배포에 대한 새 승인은 이번 검증 코드의 GitHub 반영에만 사용하며 과거 승인을 재사용하지 않는다.

### 응답 크기 제한 재현과 다음 릴리스 사전 검사

`verify-release-response-bounds.py`는 구 정식3.1.0(고정 커밋 `afc7d0b9749a42c034e7b7c7305cf8a6cd7fd23a`)의 실제 ReleaseUpdateClient/UpdatePreferences와 현재 실제 클래스를 각각 별도 JVM으로 컴파일한다. Android 설정은 메모리 대역이고 모든 HTTPS 연결은 JVM 내부 가짜 연결로 가로챈다. 네트워크·기기·로그인·운영 DB를 사용하지 않는다. 기존 JSON 테스트 라이브러리만 사용하며 새 의존성을 다운로드하지 않는다.

- 구 버전524,288바이트/현재2,097,152바이트 각각 한도−1/정확히한도/한도+1, UTF-8 다바이트, 스트림 정리 검사.
- 실제 요청/설정 저장 경로에서 초과 응답 거부, 이전 JSON/ETag/사용자 설정 보존, 안전한 오류, 재시도 회복과304 캐시 사용을 검사한다. 저장소 이름 변경을 임의 신뢰하지 않는 조건을 포함해 각각35개다.
- `--release-json <file> --budget-only`로 **익명 `/releases?per_page=30`의 새 응답 전체**를 검사한다. `/latest`나 오류 객체는 거부한다. 특정 공개 응답 크기를 기대값으로 고정하지 않는다.
- 종료0: 구 한도와 사전 여유 충족. 종료1: 구 한도 초과. 종료2: 여유32KiB 미만으로 **다음 릴리스 전 점검 필요**. 32KiB는 이번에 정한 사전 점검 여유이며 GitHub/앱의 실제 한도가 아니다. 합성 검사를 통과해도 공개 예산 검증을 완료했다고 표시하지 않는다.
- 2026-10-21 08:50 KST 익명 읽기 관측은493,841바이트/구 한도까지30,447바이트. 본문 크기는 한도 안이지만 기본 사전 여유보다 작아 종료2를 재현했다. 아래 저장소 이름 변경 문제도 별도로 확인했으므로 크기만으로 업데이트 정상이라고 판단하지 않는다. 다음 앱 릴리스는 신규 본문/자산을 포함한 게시 후 목록 크기를 별도로 확인해야 한다. 이 수치는 일시점 관측이며 영구 기준이 아니다. 기존 릴리스 삭제·본문 축약·API 경로 변경은 하지 않았다.

### 기준 불일치 교정 근거

1. `verify-insights.py`: 실제 계산/저장 검사는2181+67+533개를 통과한 뒤 옛3.0.0/code85 고정값에서 실패했다. 정식3.1.1/code88과 AppConstants 동등성으로 교정했다. 계산·저장·키 생성 방지 검사는 그대로 유지한다.
2. `verify-localization.py`: 첫 OAuthClient 실패는 데모 토큰의 refresh/revoke 네트워크 차단 두 줄이다. 이어서 가려져 있던 RefreshScheduler의 예약 차단, UsageRefreshJobService의 시작/대기 작업 차단도 확인했다. `stable311_review.py`는 이 정확한 추가 코드만 역변환하고 나머지 전체 바이트를 기존 독립 원본과 계속 비교한다. 차단 코드 수정/누락/중복 또는 그 밖의 변경은 실패한다.
3. 옛 원본에 존재하지 않는 CoachCatalog/MxpEconomy/MxpHonors는 공개3.1.1의 각 파일 SHA256에 고정했다. 기존 기준 파일을 추가/덮어써 존재했던 것처럼 만들지 않았다.
4. AppConstants·app/Wear Gradle·Manifest·UsageApi의 승인된 버전/QA 옵션/비공개 화면/성장·메달 저장 연결 차이는 공개3.1.1의 **정확한 전체 파일 SHA256**으로 검사한다. 이5파일과 신규3파일 이외에는 이 예외가 적용되지 않는다. 자동 해시 갱신 기능은 없다. 공개 승인된 값의 추가 검토 없이 파일을 바꾸면 실패한다.
5. `test_release_validation.py`의6개 테스트/하위 사례는 byte 경계, UTF-8, 잘못된 JSON/여유값, 8파일 변조, 세 런타임 코드 변조/차단 누락 및 실행 중 결과를 완료로 표시하지 않는지 검사한다. 과거 baselines 폴더는 전혀 수정하지 않았다. 한국어 리소스 대응·영어 원문·원래 파서·포맷 테스트도 그대로 실행한다.
6. `run-stable311-validation.py`에서 과거 실패 문구를 찾으면 통과시키던 두 우회를 제거했다. 이제 두 검사 모두 종료0이 필수다. `--release-json`을 주면 응답 크기 사전 점검도 필수 통과한다. CI의 기본 합성 성공은 실시간 공개 크기/실사용 성공과 구분한다.

좁은 실행 명령(JDK21/기존 JSON jar):

```text
python android/tests/test_release_validation.py
python android/tests/verify-release-response-bounds.py
python android/tests/verify-insights.py
python android/tests/verify-localization.py
python android/tests/run-stable311-validation.py --bash <Git Bash executable>
python android/tests/run-stable311-validation.py --focused --bash <Git Bash executable>
python android/tests/verify-release-response-bounds.py --budget-only --release-json <fresh-public-releases.json>
```

원본 재현/최종 로그는 `android/build/local-qa-20261021` 및 기존 `android/build/stable311-validation`에 보관한다. 실사용·실기기·실계정·Wear/Windows 실통신 검증과 로컬 Gradle/APK 빌드는 이번에 수행하지 않는다. 기존3.1.1 공개 자산/태그와 베타9.9.11·SOURCE 미커밋 변경·키/데이터는 보존한다. Drive 변경과 새 자격증명·계정·리뷰어·병렬 개발자 생성은 없다.

### 범위 밖 과거 검사에서 새로 드러난 실패

옛 insights 버전 검사가 통과한 뒤 전체 `android/run-tests.sh`는 그다음 `verify-ui-polish.py`의 `PolishSelfTest: All 64 versions offline`에서 실패했다. 64개라는 과거 고정 기록 수를 전제한 별도 검사이며 이번 두 기준 교정 범위 밖이다. 실패를 허용하는 문자열 우회를 추가하지 않고 전체 실행은 실패로 유지한다. 과거 검사 전체가 녹색이라고 보고하거나 정식main을 덮어쓰지 않는다.

이번 검사/문서 개선은 `qa/release-validation-20261021` 브랜치에서 공개 검토할 수 있게 반영하고, `.github/workflows/release-validation.yml`은 **Gradle/SDK/기기 없이** 해당 범위의 `--focused` 검사만 실행한다. 결과에는 `focused:true`, `all_legacy_checks_passed:null`을 남겨 전체 회귀 성공과 구분한다. 기존 전체 CI/검사/과거 fixture는 삭제하거나 무력화하지 않는다. 정식main 병합 및 새 앱 릴리스 전에는 이 별도 과거 검사 문제도 검토해야 한다.

### 별도 중요 발견: 현재 저장소 이름과 설치된 앱의 신뢰 경로 불일치

검증 브랜치 push 시 GitHub가 저장소 이동을 안내했다. 읽기 전용 API로 저장소 ID1411531926의 현재 이름이 `ldjkorea/DJS-CODEX-METER`이며 main은 여전히6638a50인 것을 확인했다. **이번 작업에서 이름을 바꾸거나 원래대로 돌린 것은 아니다.** 현재 익명 릴리스 JSON의 html_url/다운로드 URL도 새 이름을 사용한다.

이 익명 응답을 설치된 정식3.1.1과 동일한 실제 GitHubReleaseParser/UpdateChannel에 넣은 별도 JVM 재현은 `rows=0; tracked=NONE`이었다(`android/build/local-qa-20261021/public-parser-probe.log`). 현재 앱은 옛 `/ldjkorea/codex-meter-ko/releases/`만 신뢰하므로 HTTP/API 리다이렉트가 성공해도 새 주소의 릴리스를 선택하지 못한다. 10월17일의 실제 업데이트 성공 증거를 현재 경로 정상의 증거로 재사용하면 안 된다.

이는512KiB 경계와 독립적인 배포 호환성 문제다. 허용 저장소 경로 확대/저장소 이름 복원/실사용 앱 교체는 이번에 하지 않는다. 보안권한 확대 금지 범위를 유지하고, 해결 전 새 앱 자동 업데이트가 정상이라고 보고하지 않는다. 이번 테스트의 새 가짜 renamed-URL 조건도 이를 임의 허용하지 않는 기존 동작을 확인한다. 익명 메타데이터에서3.1.1 APK의 기존 크기24,787,898와 SHA2560c1ee6a5fc84da46ace3012053239098ac30f660ed1efc5b3760b1c36ed3213c는 유지됐지만, 이번에 APK 재다운로드/설치 검증은 하지 않았다.
