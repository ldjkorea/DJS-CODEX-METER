# GPT HUD 검증 순서

새 테스트 프레임워크를 도입하지 않는다. 기존 self-test/source guard/SQLite/native 실행기를 유지하며 `run-release-validation.py`가 전체 회귀·현지화·폰트 검사를 한 번씩 순서대로 실행한다. 실패한 단계는 종료 코드와 별도 로그로 남기며 이후 미실행 단계는 통과로 기록하지 않는다.

| 범주 | 기존 검사/근거 | 실제 실행 범위 |
|---|---|---|
| 인증·토큰·API | ParserSelfTest, verify-release-stability, verify-localization | JVM/소스; 실제 OAuth는 별도 |
| 파싱·정밀도 | ParserSelfTest, verify-insights, verify-live-cache | JVM/표시 계약 |
| 리셋·크레딧·원장 | verify-sqlite, verify-lan-merge | 생산 DAO + 합성 SQLite |
| 결제·월간 가성비 | verify-subscription-store, verify-phase-a, verify-rpg-step1 | 합성 결제/정책 경계 |
| RPG XP·티어 | verify-rpg-step2, verify-rpg-store | 엔진/DAO/운영 갱신 fixture |
| 독립 ON/OFF | verify-achievement-journey, GrowthRebornNativeTest | 합성 엔진 + 실제 Activity |
| RPG 암호화 복원 | refinement/store 검사, PhaseCNativeTest, PhaseEUpgradeNativeTest | 실제 암호화/Android SQLite; SAF 동선 별도 |
| 홈·기록·성장 | verify-ui-polish, PhaseANativeTest, 성장/도감 native | 리소스 + 실제 코드 View 렌더 |
| 두 주요 위젯 | verify-widget-simplification, PhaseBNativeTest/PhaseCNativeTest | 합성 RemoteViews/계정·실패 상태 |
| Samsung/Now Bar/Wear | ParserSelfTest, shared 계약, PhaseCNativeTest, Wear 빌드 | 에뮬레이터/컴파일; OEM 실기기 별도 |
| Windows·PC 연결 | windows-widget/build.ps1 -Test, verify-rpg-windows, verify-lan-sync | Windows 실행/오프스크린/TLS 루프백 |
| 업데이트·서명 | RpgReleaseSelfTest, PhaseEUpgradeNativeTest, apksigner, verify-publication | 파서·격리 업데이트·서명·폰트/패키지 |
| 로컬 프로필 공유 | verify-profile-share, ProfileShareNativeTest | 허용 목록·PNG metadata·실제 Activity/FileProvider/저장 stream/chooser 취소; 실제 수신 앱 별도 |

차기 로컬 후보의 업데이트 체인은 UpgradeContinuityNativeTest로 v51의 일회성 수집 초기화 뒤 재시작한 컬렉션이 다음 업데이트에서 다시 초기화되지 않는지 확인한다. 후보 snapshot은 production 소스와 동일하며 QA applicationId만 별도다. 기존 사용자 DB·인증·서명을 조작하지 않는다. 문서 선택 UI/실제 친구 전송/삼성 호스트 성공은 별도 증거가 필요하다.

## 실행 순서

1. Git/원격 기준과 기존 미커밋 파일을 확인한다.
2. 작은 수정은 해당 기존 `verify-*.py`를 먼저 실행한다. JVM 공통 산출물이 필요하면 현재 소스로 전체 회귀를 먼저 실행한다. 과거 통과 로그를 새 검증으로 재사용하지 않는다.
3. 최종 소스에서 `python android/tests/run-release-validation.py` 실행. Windows는 `--bash "C:/Program Files/Git/bin/bash.exe"`. 로그/결과 JSON은 `android/build/release-validation/`에 보관한다.
4. 격리 QA APK를 만들고 native 실행기로 업적·칭호·위젯·백업을 검증한다. 구버전 코드 → v30 → 최종 코드의 같은 QA 패키지에 `install -r`만 사용한다. 기존 실제 앱은 삭제/초기화하지 않는다.
5. 최종 일반 Phone/Wear Release·lint, Windows 빌드·루프백 테스트를 수행한다. QA 속성 없는 APK의 패키지·버전·서명을 다시 확인한다.
6. `verify-publication.py <Phone APK> <Wear APK>`로 최종 APK의 라이선스/폰트를 확인한다. 소스/공개 archive 민감정보 검사는 별도로 수행한다.
7. 최종 커밋으로 GitHub Actions가 통과한 뒤에만 draft 자산·체크섬을 검증하고 정식 Release를 게시한다. 게시 후 실제 metadata/다운로드와 생산 updater 파서를 재검증한다.

실제 PC/휴대폰 계정 DB·토큰·페어링 코드·원본 세션 로그를 테스트 로그/캡처에 넣지 않는다. 에뮬레이터의 합성 성공은 삼성/Wear·실계정 OAuth·실제 Tailscale 연결 성공과 다르다. 테스트 수가 많다는 이유로 미검증을 완료 처리하지 않는다.
