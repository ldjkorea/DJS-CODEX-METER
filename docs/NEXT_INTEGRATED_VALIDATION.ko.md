# GPT HUD 차기 통합 검증 결과

2026-10-13. 안정화 `5a1840d`, 로컬 공유 및 업데이트 검증 `037cd0c`. 기준 공개 main/Release는 재조회에서도 `0f51e31`/v2.8.51, Windows 0.1.10이다. 공개 변경은 하지 않았다. `docs/emblem-concepts/`와 이전 작업본은 보존했다.

## 실제 구현과 보존

계정 변경 때 성장 UI의 이전 계정 화면/콜백을 격리했다. APK 폰트 검사는 실제 manifest로 phone/Wear를 분류한다. 날짜 경계 및 additive audit/신선도에 대한 잘못된 테스트 가정을 수정하고 회귀 검증을 강화했다. 성장 화면에는 읽기 전용 프로필 이미지 미리보기/저장/명시적 공유를 추가했다.

SQLite/저널 스키마, 원장/결제/RPG 계산식, 기존 업적/칭호 ID, 수집 revision, OAuth/API 요청, LAN/Tailscale 프로토콜, Windows/Wear UI/사용자 설정, 패키지와 서명키는 바꾸지 않았다. 실제 사용자 DB에 접근하거나 수정하지 않았다.

## 자동 게이트

최종 소스에서 실행:

```text
python android/tests/run-release-validation.py --bash "C:/Program Files/Git/bin/bash.exe" --apk android/app/build/outputs/apk/release/app-release.apk --apk android/wear/build/outputs/apk/release/wear-release.apk
gradlew -p android :app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease --console=plain
windows-widget/build.ps1 -Test
apksigner verify --print-certs app-release.apk
```

| 검사 | 결과 | 증거 |
|---|---|---|
| 전체 기존 Android/JVM/source/SQLite/RPG/LAN 회귀 + 신규 공유 | PASS | release-validation/regression.log |
| 전체 현지화/기존 English/formatter 및 보존 계약 | PASS | localization.log |
| 폰트/라이선스/원본 출처 및 최종 Phone/Wear APK | PASS | publication.log, next-apk-verification.log |
| Git index 민감정보·런타임/키 추적 검사 | PASS | public_source.log; 전체 과거 Git 보안 감사와는 다름 |
| Windows 빌드/인증·TLS 루프백/오프스크린 | PASS | 291 assertions, 112 renders; stability-windows.log |
| Android/Wear Release 및 lint | PASS | next-final-build.log; baseline 확대 없음 |
| 프로필 공유 모델 | PASS | 14 checks; 실제 허용 목록/unknown/선택 |
| 프로필 공유 native | PASS | 30 checks; 실제 Activity/PNG/FileProvider/취소/IO 실패/계정 교체 |
| 프로필 PNG 개인정보 metadata | PASS | 10 합성 PNG, text/EXIF chunk 부재 및 주입 차단 |
| 성장 계정 전환 native | PASS | 7 checks; blocked worker/늦은 콜백 |
| 생산 Android SQLite/암호화/복원/RemoteViews native | PASS | 최종 QA에서 PhaseCNativeTest 87 checks |
| 일회성 수집 초기화/독립 ON/OFF/복원/6작업 native | PASS | 최종 QA에서 CollectionRestartNativeTest 31 checks |

원시 로그/RESULTS.json은 `android/build/`에 있다. native QA는 실제 앱 클래스를 격리 packageId로 실행한다. 가상 Android framework만으로 완료 처리하지 않았다.

## 설치 유지 업데이트 체인

같은 합성 package `dev.bennett.codexmeter.phaseeqa`와 같은 에뮬레이터에서 v28 → v30 → v50 → v51 → 현재 소스 snapshot을 `install -r`했다. 실제 production 앱 삭제·데이터 초기화·다운그레이드는 없었다. 현재 snapshot에서는 테스트용 applicationId만 변경했다.

- v28 fixture 7 checks, v30 checkpoint 10, v50 verify 26 PASS.
- v51 수집 시작 이후 checkpoint 9 PASS, 현재 후보 12 PASS.
- 로그인 암호 blob, XP/레벨/티어/최고 기록, 컬렉션/과거 보관함, 대표 메달 순서, 칭호, 결제·관측 근거, SQLite 행, 기존 위젯 binding/설정, 암호화 PC 페어링 보존 확인.
- v51에 승인됐던 한 번의 OFF·0 전환을 확인했고, 그 후 수집을 다시 ON으로 한 상태는 후보 업데이트에서 유지됐다.
- 초기 v50 JSON 완전 일치 실패는 새 calculation audit가 추가된 차이였다. 기존 이력/소비/금액을 모두 보존하는 더 정확한 검사로 재실행했다.
- 첫 token summary만 넣은 준비 단계는 새 수집 baseline이어서 메달이 없었다. 추가 증가 관측을 제공하고 삭제 없이 제한된 resume 경로로 이어갔다.
- 후보에서 demotion freshness 진단 2개만 시각 경과로 바뀌었다. 원본을 수정하지 않고 비교용 사본에서 그 두 진단을 제외해, 모든 성취·계산 필드 불변을 검사했다.

## 화면과 성능

실제 렌더: 프로필 320/360/411dp, 한국어 light 320dp 1.5배, 영어 dark 411dp 2배, 실제 PNG dark/light/빈 상태/합성 Diamond 예시. `android/build/profile-native/captures/profile-fixtures/`. 수출 카드의 게임형 평가 고지와 메달·칭호 이름이 표시되는 것을 확인했다. 모든 기존 화면의 700dp/TalkBack/회전 매트릭스를 새로 완주한 것은 아니다.

동일 emulator/합성 데이터에서 MainActivity TotalTime 5회: 이전 [1002,778,694,946,950]ms, 수정 [815,950,719,918,927]ms, 중앙값 946/918ms. 변동이 크므로 성능 향상률은 주장하지 않는다. raw JSON과 meminfo는 stability-performance.json, stability-memory-before/after.txt다. 공유 추가 뒤 모든 화면·메모리 누수의 별도 비교 측정은 하지 않았다.

기존 캐시의 native 합성 12회 요청에서 사용량/작업 위젯 재구성이 각각 12→1로 제한되는 것을 확인했다. 새로 만든 최적화나 삼성 Binder/배터리 절감 실측으로 해석하지 않는다.

lint: Phone 261 warning+2 hint, Wear 11 warning, error 0. baseline의 숨겨진 항목까지 해결했다는 뜻이 아니다. 기존 unused resource/SmallSp 등은 남아 있고 관련 없는 대규모 정리는 수행하지 않았다. Gradle deprecation 및 native debug symbol strip 안내도 남는다.

## 실패·환경 제약

초기 자정 LAN fixture 실패, manifest/Wear 파일명 분류 실패, 새 ProfileCard/manifest 보존 검사 미등록은 원인 확인 뒤 수정·재검증했다. 기존 fixture 기대 소비나 계산 규칙을 낮추지 않았다. 과거 immutable fixture 자료는 변경하지 않았다.

QA snapshot 복사에서 Windows 긴 경로 제한이 발생해 long-path-aware 복사로 해결했다. 잘못 복사된 snapshot은 검증 결과물로 쓰지 않았다. native 재실행 중 emulator-5554가 사라져 연결된 기존 emulator-5560에서 최종 QA를 이어갔다. 실기기 연결로 해석하지 않는다. 임시 snapshot/원시 synthetic JSON/키 junction은 ignored/local 경로에만 있으며 공개 파일에 포함하지 않는다.

## NOT VERIFIED 및 다음 판단

실제 삼성 업데이트/로그인/API, Samsung 홈·AOD·Now Bar, 실 Wear, 실사용 Windows↔폰/외부망 Tailscale, 장시간 Doze·배터리·재부팅, 다중 Android OS의 SAF/Quick Share/카카오톡 수신, 프로세스 종료 중 문서 선택, 전체 기존 화면 접근성/메모리 누수: NOT VERIFIED.

자동화에서 미해결 데이터 손실/계정 격리 실패는 발견되지 않았다. 실기기 부재는 자동 검증의 범위를 넓혀 주장할 근거가 아니다. 새 외부 서버/유료 API/DUEL/Tibo 수집을 추가하지 않았다. 타 서비스 인터페이스는 타당성 문서에 조건부/불가능/비추천을 구분했다.

판정: 차기 코드와 로컬 컴파일/검증 준비 완료. **공개 배포 보류**. 정식 버전/code 정합성, 실제 기기 QA 결과와 제한사항, 최종 소스/서명/체크섬을 확정한 뒤 사용자의 공개 push/tag/release/APK 승인 필요.

내부 phone SHA-256 `597066c2d5b3a30e7542ed432cce35e0c869de590c77ed1f148b98fb3750ba1d`, Wear `2eb64a031ee48b2a8b9c2965ce8ea177612954514c5effd6a61dd91e9c1b1a8c`. Phone 패키지 `dev.bennett.codexmeter`, metadata 2.8.51/81이며 기존 signer `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`를 확인했다. 이 APK들은 승인된 신규 업데이트 배포 파일이 아니다.
