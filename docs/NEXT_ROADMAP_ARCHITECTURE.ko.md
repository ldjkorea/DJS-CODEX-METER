# GPT HUD 차기 개발 및 아키텍처 결정

2026-10-13. 공개 기준 Android/Wear 2.8.51/81, Windows 0.1.10, main `0f51e31`. 독립 로컬 브랜치 `feature/stability-profile-share`. 이번 변경은 공개 push/tag/release/운영 앱 업데이트를 포함하지 않는다.

## 순서와 판정

1. 내부 2.9.0: 계정 변경 UI 경합, APK 검증 분류, 날짜 경계 테스트를 수정한다. 기존 저장·계산식을 바꾸기보다 실제 결함과 검증 사각지대를 고친다.
2. 내부 2.9.1: 안정화 자동 게이트 통과 후 성장 화면에 로컬 프로필 이미지 기능을 별도 커밋으로 추가한다.
3. 3.0: 타 서비스와 프로젝트별 관측의 타당성만 조사한다. 미확인 인터페이스를 구현하지 않는다.
4. DUEL: 연구 문서만 보존한다.

공개 빌드 메타데이터는 2.8.51/81을 유지했다. 내부 목표 숫자는 출시 완료를 뜻하지 않는다. 승인 후 새 versionCode, 앱/Wear 버전, 기존 updater의 증가 조건을 별도로 맞춰야 한다. 같은 코드의 내부 APK를 기존 공개 파일과 바꾸거나 배포하지 않는다.

## 결정

| 결정 | 이유와 범위 |
|---|---|
| 기존 SQLite/JSON 스키마 유지 | 공유는 기존 저널의 읽기 전용 투영이다. 복원/XP/월간 결산을 새 DB로 이동할 이유가 없다. |
| ProfileCard 허용 목록 | 원본 저널 대신 닉네임·유효 티어·레벨·선택한 획득 메달·획득 칭호만 분리한다. 계정 ID를 전달 객체에 넣지 않는다. |
| 단일 로컬 작업자 + 화면 generation | PNG 생성과 파일 IO를 UI 스레드에서 빼고 계정 교체/종료 뒤 콜백을 차단한다. 새 서버나 SDK가 필요 없다. |
| 기존 Pretendard·휘장 재사용 | 정체성과 라이선스를 유지하고 대형 이미지 추가를 피한다. |
| 좁은 FileProvider | `cache/profile-share/`만 읽기 URI로 공유한다. DB·백업·OAuth 폴더는 노출하지 않는다. |
| 공유·저장을 명시적 버튼으로 제한 | 미리보기 진입은 외부 전송이 아니다. Sharesheet가 열렸다고 전송 완료를 주장하지 않는다. |
| LAN/Tailscale 유지 | 친구 연결이나 프로필 공유를 기존 인증 채널에 섞지 않는다. |
| 근거 없는 성능 리팩터링 제외 | 콜드 스타트 변동이 큰 상태에서 개선률을 만들거나 저장소 전체를 바꿀 근거가 없다. |

## 기존 경계

UsageApi/UsageParser → UsageLedgerDatabase → AccountProgress/MonthlyLedger → RpgProgressStore/RpgEngine의 흐름을 유지한다. 서버 한도와 PC 처리 토큰은 서로 다른 단위와 관측 범위다. 토큰·게임 점수로 생산성/절약액을 새로 산출하지 않는다.

공유 흐름은 RpgProgressStore + RpgShowcase → ProfileCard → ProfileCardRenderer → 화면 미리보기/SAF/명시적 Android 공유다. 공유 이미지는 계정 간 자동 동기화 데이터가 아니며 복원 자료로도 사용하지 않는다.

## 출시 전 조건

자동검증, 에뮬레이터, 실제 삼성/Wear/외부망 검증은 독립된 증거다. 미검증 항목은 수동 QA 문서에 남긴다. 최종 버전 결정·public push·tag·release·서명 APK 공개는 사용자에게 별도 승인을 받아야 한다. 서명 개인키는 기존 로컬 경로에 유지하고 CI에 전달하지 않는다.

관련 결과: [안정화](STABILITY_2.9.0.ko.md), [프로필 공유](PROFILE_SHARE_2.9.1.ko.md), [플랫폼 타당성](HUD_PLATFORM_3.0_FEASIBILITY.ko.md), [DUEL 연구](DUEL_RESEARCH_BACKLOG.ko.md), [수동 QA](NEXT_DEVICE_QA.ko.md), [통합 검증](NEXT_INTEGRATED_VALIDATION.ko.md).
