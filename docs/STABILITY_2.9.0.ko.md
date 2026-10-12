# v2.9.0 안정화 — 로컬 개발 게이트

기준: 2026-10-13, 공개 v2.8.51/code81 `0f51e31`, Windows 0.1.10. 원격 main과 최신 Release가 기준과 일치함을 실제 조회했다. 브랜치 `feature/stability-profile-share`; 기존 `docs/emblem-concepts/` 보존. 공개 버전 메타데이터는 아직 2.8.51/81이며 이 내부 빌드를 배포하지 않는다. 최종 버전/코드는 배포 승인 뒤 별도 정합성 검사한다.

## 확인한 결함과 수정

- GrowthActivity가 busy일 때 계정 변경을 확인하기 전에 반환했다. 이전 프로필이 작업 완료까지 남고, 오래된 콜백이 새 작업의 busy를 해제할 수 있었다. 계정 경계를 먼저 확인해 화면/상세/스크롤을 비우며 generation이 맞는 콜백만 상태를 바꾼다. 계산과 저장 포맷은 변경하지 않았다.
- LAN SQLite same-day 테스트가 현재 시각에서 1시간 전도 같은 날짜라고 가정했다. 자정 직후 실패를 실제 재현했다. 같은 날짜의 유효 시각을 사용하며 기존 날짜 경계·미배분 소비 검사를 유지한다. 기대 소비량이나 production 산식을 완화하지 않았다.
- APK font 검사가 파일명에 `Wear`가 있는지로 분류했다. 원래 Gradle 산출물 `wear-release.apk`를 휴대폰으로 오인했고 이름 변경으로 검사 분기가 바뀔 수 있었다. 실제 aapt2 manifest의 watch feature를 읽어 분류한다. 폰트/라이선스 원본 및 금지 해시 검사는 그대로다.
- README에 이미 제거된 소수점 설정 안내가 남아 있어 실제 정수 표시와 원본 정밀도 보존 정책에 맞췄다.

## 실행 근거

- `python android/tests/run-release-validation.py --bash "C:/Program Files/Git/bin/bash.exe"`: 전체 JVM/source/SQLite/계정/RPG/Windows/LAN TLS 루프백, 현지화, 출처, Git index 검사 통과. 초기 LAN 테스트 실패는 위에 기록했고 수정 후 재실행했다.
- `gradlew :app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease`: 통과. 기존 lint baseline을 확대하지 않으며 경고 0을 의미하지 않는다.
- `verify-publication.py app-release.apk wear-release.apk`: manifest 분류 수정 뒤 두 APK 폰트/라이선스/금지 해시 검사 통과.
- `windows-widget/build.ps1 -Test`: 291 assertions/112 offscreen renders 통과. classic PowerShell 실행 정책 차단은 시스템 정책 변경 없이 현재 PowerShell에서 재실행했다. 사용자 Windows Widget 실행/설정은 변경하지 않았다.
- StabilityNativeTest: production GrowthActivity의 worker를 실제로 차단한 상태에서 로그아웃/계정 교체와 늦은 콜백을 검사, 7개 통과.
- PhaseCNativeTest: 실제 Android SQLite/암호화/복원/위젯 합성 87개 통과.
- CollectionRestartNativeTest: 실제 SQLite/Activity의 최초 OFF·0/재접근/복원/독립 시작/6작업 31개 통과.
- 동일 emulator-5554, 동일 QA 패키지와 합성 저장 데이터에 v51 QA → 후보 QA를 `install -r`했다. 사용자 production 앱/DB를 열거나 수정하지 않았다.

원시 로그: `android/build/stability-*.log`, `android/build/release-validation/`. 프로필 공유 추가 후 최종 통합 소스로 다시 검증한다.

## 측정과 한계

MainActivity `am start -W` TotalTime 5회: 이전 [1002,778,694,946,950]ms / 수정 [815,950,719,918,927]ms. 중앙값 946/918ms. 원시 JSON과 dumpsys meminfo는 `android/build/stability-performance.json`, `stability-memory-before/after.txt`. 변동이 크므로 속도 향상률·배터리 절감률을 주장하지 않는다. 다른 non-exported Activity의 adb 직접 실행은 거부되어 측정에서 제외했다. manifest를 약화하지 않았다. 도감 스크롤/메모리 누수/장시간 배터리의 신규 비교 측정은 미완료다.

## 데이터와 미검증

SQLite 스키마·OAuth 요청·한도/결제/RPG 산식·수집 revision·업적/칭호 ID·provider·Windows 연결 형식은 변경하지 않았다. 기존 초기화의 일회성/반복 복원은 합성 원장 및 native 검사로 확인한다. 이번 기기 업데이트 비교는 v51→후보이며 v30→v50→v51 전체 체인의 신규 재실행은 후속 QA 체크리스트에 남긴다. 기존 성공 보고서를 새 실행 근거로 재사용하지 않는다.

Samsung 설치/런처/AOD/Now Bar, 실제 Wear, 실계정 OAuth/API, 실제 PC↔폰/Tailscale, Doze/재부팅: **NOT VERIFIED**. 연결된 장치는 에뮬레이터 3개뿐이다. 공개 push/tag/release/upload는 수행하지 않는다.

판정: 필수 로컬 자동 게이트 통과, Phase B의 독립 로컬 개발 진행 가능. 실기기 인증·전체 구버전 체인 QA 완료나 배포 승인으로 해석하지 않는다.

## 최종 통합 시 추가 검증

위 내용은 안정화 첫 커밋 시점의 범위다. 이후 별도 합성 QA 패키지에서 v28→v30→v50→v51→현재 후보를 실제 `install -r`로 이어서 검증했다. v30/v50 단계의 XP·로그인·위젯·페어링·결산과 v51 초기 수집 재시작, 후보에서의 XP/티어/수집 ON/OFF/장착값/결제/관측 원장 보존을 확인했다. 실제 사용자 설치본을 변경하지 않았다.

추가 검사에서 다음 테스트 가정도 수정했다. 과거 fixture 파일과 생산 계산식은 변경하지 않았다.

- v30→v50: 계산 fingerprint 강화로 감사 이력 한 건이 추가됐다. 소비량/금액/결제 구간과 이전 이력은 동일했다. 전체 JSON 완전 일치 대신 모든 기존 계산 근거의 prefix 보존과 나머지 결산 필드의 완전 일치를 요구한다.
- v51의 첫 post-start 토큰 요약은 새로운 기준 관측이다. 증가 소비를 입증하는 두 번째 관측을 넣어 실제 수집을 검사한다. 첫 관측만으로 과거 토큰을 소급 지급하도록 생산 로직을 바꾸지 않았다. 실패한 부분 fixture는 삭제 없이 명시적 resume 조건으로 재개했다.
- 후보 업데이트 중 관측이 오래돼 demotion_input/checked_at 진단값이 바뀌었다. 두 시간·신선도 진단만 비교용 사본에서 제외하고 모든 티어/최고 기록/XP/업적/칭호/결산/원장 필드는 동일함을 검사한다. 저장 원본을 수정하거나 강등 조건을 완화하지 않았다.

통합 결과·실패 로그·최종 미검증 범위는 [통합 검증](NEXT_INTEGRATED_VALIDATION.ko.md)에 남긴다. 공개 배포 승인은 별도다.
