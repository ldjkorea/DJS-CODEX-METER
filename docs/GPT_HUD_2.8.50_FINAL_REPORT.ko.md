# GPT HUD 2.8.50 통합 검증 보고서

작성일: 2026-10-12. Phone/Wear **2.8.50 / versionCode 80**, Windows **0.1.10**.

## 결과와 배포 게이트

로컬 필수 검증, GitHub Actions, draft 자산 재다운로드, 공개 다운로드 및 생산 업데이트 파서 검증을 통과해 **v2.8.50 정식 Release 한 번을 게시했다**. 확인된 P0/P1 차단 결함은 없다. 실제 사용자 휴대폰에 설치하거나 사용자 DB를 복구한 상태는 아니다.

공개 Release: [v2.8.50](https://github.com/ldjkorea/codex-meter-ko/releases/tag/v2.8.50). 중간 2.8.31~49 Tag·Release·APK를 생성하지 않았다. 이전 공개 파일을 보존했다.

## 통합 기준과 코드

- 통합 전 공개 main/Tag: `43ed9031c53abf863d4c8b239d06172850bfe38c` / v2.8.30 / code60. 오래된 원본 작업 폴더는 사용하지 않았다.
- Phase A `78e3634`, B `5001e8e`, C `62c127a`, D `ebcf5ed`가 같은 계보에 있음을 확인했다.
- Phase E 코드·검증·버전·화면 자료 커밋: `ee75a91d22515e163acaedc3ffd7137ce630e268`. 최종 배포 소스는 v2.8.50 Tag가 가리키는 커밋이다. 이후 보고서 보완은 앱 코드 변경과 분리한다.
- 독립 작업 브랜치 `feature/development-phase-e`. 기존 미추적 `docs/emblem-concepts/`, 다른 checkout, 서명 개인키·사용자 DB·설정을 보존했다.

## v2.8.30 대비 실제 변경

1. 홈의 데이터 최신성 안내와 기록의 달력/추세 동선을 정리하고 로그인 후 선택 기능 안내를 추가했다. 영역 순서·숨김 설정을 유지한다.
2. 원본 소수값 변화, 미래 PC 관측, 시계 역행과 역순 상태 처리를 보완했다. 없는 소비량·추정 소수점을 생성하지 않는다.
3. 결제 기간 결산 근거와 수정 이력의 일치 여부를 확인한다. 주간/전역 리셋이 과거 월간 결산·최고 티어를 지우지 않는다. 환산액은 실제 API 비용·절약액이 아니다.
4. 업적·칭호 OFF/ON 세그먼트를 보호하고 OFF 기간을 소급하지 않는다. 기존 XP·53개 업적 정의·과거 자동 메달 보관함·대표 메달·장착 칭호를 보존한다.
5. 위젯 렌더 캐시와 동일 정책 주기 예약, 같은 도감 데이터 재로딩을 보완했다. 큰 글꼴의 필터, 차트 접근성 클릭과 화면 재생성을 검증했다.
6. 암호화 RPG 복원, 일반 설정의 인증 내보내기 차단, 업데이트 설치 직전 파일 재검증을 보완했다. package/provider ID·SQLite 스키마·OAuth 요청 형식은 유지한다.
7. Windows 작업/원장 전송의 영속 재생 검사와 공유 메타데이터 검증을 통합한다. Windows UI와 기존 LAN/Tailscale 인증 형식·AppData는 유지한다.

UI 비교 기준은 이전 `docs/widget-simplification-2.8.30/`와 새 `docs/integration-2.8.50/`이다. 새 자료는 실제 Activity 코드·합성 계정의 렌더링이다. 좁은 화면/큰 글꼴은 1열로 전환하고 핵심 상태는 요약, 상세는 클릭·스크롤로 제공한다.

## 검증 결과

| 검사 | 결과와 범위 |
|---|---|
| `run-release-validation.py --bash ... --apk <Phone> --apk <Wear>` | 기존 전체 self-test/소스·SQLite 회귀, 현지화, 최종 APK 폰트·라이선스, Git index 검사 모두 종료 코드 0 |
| 한국어·영어 | 앱 1,680 strings/71 array items, Wear 116/13. formatter 629·5시간 초기화 44. 리소스 대칭 통과 |
| 구버전 업데이트 | 공개 v28 코드(code58) → v30(code60) → 통합본(code80), 같은 격리 QA ID에 `install -r`만 사용. legacy 7/checkpoint 10/verify 23 검사 통과 |
| Phase A native | 97 검사: 실제 홈·기록·선택 기능·합성 결제/관측 |
| Phase B native | 873 검사: 1~6개 작업, 네 번째~여섯 번째 변경, 역순·실패·오래됨, RemoteViews |
| Phase C native | 87 검사: 암호화/SQLite/인증 실패 보존·설치 직전 변조 거부·위젯·플랫폼 렌더 |
| Phase D native | 113 검사: 도감 재사용·검색/재생성·차트 접근성·예약·1만 SQLite 관측 |
| 성장 시작/재개 | 133 native 검사. 네 가지 ON/OFF 조합·레거시 XP/최고 티어·취소/시작·기간 경계 등 |
| Phase E UI | 242 검사 / 56 Activity 캡처. 320/360/411/700dp, 1.0/1.5/2.0 글꼴, 한국어·영어, 라이트·다크의 대표 조합. 요청 언어·글꼴 실제 적용을 별도 확인 |
| Windows | 291 assertions / 112 offscreen renders. 실제 코드 컴파일·합성 데이터 |
| LAN | Java/C# 고정 인증서 TLS 루프백 56 검사. 합성 연결, 실제 휴대폰/외부망 아님 |
| Gradle | Phone/Wear Release·lint 성공. QA 속성 없는 공개 패키지를 별도 검증 |
| lint | Phone 신규 노출 error 0·247 warnings. 기존 baseline 53 errors/111 warnings 필터 유지. Wear 0 errors/11 warnings. baseline 확대 안 함 |
| 소스 개인정보 | Git index의 비밀키/런타임 파일·알려진 토큰 패턴 부재. 전체 역사/보안 감사 완료를 뜻하지 않음 |

원장/월간/RPG 계산은 기존 전체 JVM·생산 DAO 합성 SQLite·운영 갱신 fixture로 검증했다. 계정 범위/서비스 일별 시간대/완전한 비교 구간을 입증하지 못하면 강등을 보류한다. 강등 보류를 실제 저활용 0으로 처리하지 않는다. 실제 계정의 과거 복구액이나 27억 토큰을 새로 확인했다고 주장하지 않는다.

## 업데이트·백업 데이터 보존

- synthetic A/B 계정의 실제 Android SQLite와 Keystore 저장을 사용했다. 로그인 암호화 저장값·XP·53개 레거시 정의·독립 시작 시각/세그먼트·ON/OFF·대표 메달 3개 순서·장착 칭호·결제/결산·원장 관측 수·기존 위젯 binding/options·암호화 PC 페어링을 확인했다.
- 암호화 백업을 Android 저장 경로로 반복 복원해 XP 중복이 없음을 확인했다. 잘못된 암호·변조 파일·다른 계정 복원은 기존 데이터를 보존하며 거부했다.
- 새 QA 에뮬레이터 실행 명령이 자동 승인 검토에서 정책 차단되어 기존 emulator의 별도 QA ID를 사용했다. 사용 중인 production 패키지는 수정·설치·제거하지 않았다.
- 구버전은 공개 Git 소스를 재빌드한 **격리 QA APK**다. 실제 공개 production APK를 실기기에 설치한 검증이 아니다. 실제 공개 v30 APK의 해시·패키지·code60·인증서를 읽어 최종 code80과 비교했다.
- 실제 물리 기기 변경·앱 삭제 후 재설치·SAF 파일 선택 전체 동선은 미검증이다. RPG 백업은 로그인이나 PC 페어링을 담는 전체 계정 백업이 아니다.

## 측정 결과 — 같은 emulator/QA 자료

| 항목 | 공개 v30 코드 | 통합 v50 코드 | 해석 |
|---|---:|---:|---|
| 콜드 실행 `am start TotalTime`, 5회 중앙값 | 1,343ms | 1,221ms | 이 환경 관측값; 보편 개선률 아님 |
| HOME 복귀 `WaitTime`, 5회 중앙값 | 51ms | 57ms | 소폭 증가. 첫 프레임/렌더 시간 아님 |
| 홈 Activity launch-to-idle 중앙값, 3회 | 751ms | 730ms | 프로세스 웜; 변동성 있음 |
| 기록 Activity 중앙값 | 721ms | 727ms | 소폭 증가 |
| 성장 Activity 중앙값 | 694ms | 744ms | 증가, 더 빠르다고 주장하지 않음 |
| 도감 Activity 중앙값 | 758ms | 727ms | 같은 합성 기록 |
| 같은 데이터 도감 전체 reload | 12/12 | 0/12 | 불필요한 조회 제거 확인 |
| 같은 메달 슬롯 재생성 | 12/12 | 0/12 | 선택·상태 변경은 정상 갱신 |
| 동일 정책 Job 등록 교체 | 12/12 | 0/12 | 기존 기한 보존 확인 |
| 메달 스크롤 total-duration p95 | 19.12ms | 18.38ms | 88 frames, 단일 샘플; 기기마다 다름 |
| SQLite 1만 관측 조회 중앙값 | 274ms | 194ms | 전체 행 보존; 반복 측정 변동 존재 |
| 프로세스 PSS | 185,756KiB | 203,200KiB | 증가. 메모리 절감 주장 안 함 |

통합본에서 12회 동일 위젯 요청의 렌더 생성은 12→1. usage 136.74→44.80ms, tasks 50.85→2.28ms. 같은 코드의 uncached/cached 요청 비교이며 Samsung Binder 비용이나 배터리 절감률이 아니다. 종료 Activity weak references는 UI 검사 후 0/56으로 관측됐지만 장기간 메모리 누수 인증이 아니다.

## 최종 파일과 서명

| 파일 | bytes | SHA-256 |
|---|---:|---|
| `CodexMeter-2.8.50-ko.apk` | 27,039,225 | `f0d3c4089e0e28f2fd3dea9ec0d88535ea7995676b12cd32bd1d8f9dac7553df` |
| `CodexMeter-Wear-2.8.50-ko.apk` | 16,142,789 | `3f3aad972acc8c742e8315d0a2e9165993d0dda6da3c2d893edc6e9ef07b0384` |
| `CodexMeter-WindowsWidget-0.1.10.zip` | 5,479,509 | `60a3526e9821210b3a3b51e214081b822dd29fc94ef3972992926cc02fb01492` |

Phone/Wear package: `dev.bennett.codexmeter`. 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`. 최종 APK와 기존 공개 v30의 인증서 일치. 새로운 키를 생성하지 않았다. Windows ZIP의 정확한 SHA-256은 릴리스 `SHA256SUMS.txt`에 포함한다. Windows Authenticode 서명은 없으며 기존 업데이트 경로의 체크섬·실행 파일 버전을 확인한다.

## 미검증·알려진 제한

- 실제 갤럭시 Samsung 홈·잠금화면·AOD·Now Bar, Wear 장치, 실계정 OAuth·사용량 API와 실제 사용자 DB.
- 실제 외부망 Tailscale, 절전/재부팅·장시간 백그라운드·배터리. 표시 갱신 감소를 배터리 절감률로 환산하지 않는다.
- 물리 폴더블/OS 전체 회전·TalkBack 사용 동선의 완전한 검사. resource 렌더/클릭/화면 재생성 검사를 실제 호스트 인증으로 해석하지 않는다.
- 공식 서비스 토큰의 완전한 시간대·계정 전체 관측 범위가 부족하면 티어 강등은 안전하게 보류된다.
- Windows 다중 파일 배치는 전체 파일을 하나의 트랜잭션으로 원자화하지 않는다. 기존 백업/실패 보존 경로를 유지하며 범위 밖 구조 변경을 하지 않았다.
- 과거 baseline lint 오류/경고가 남아 있다. 전체 lint 오류가 0인 앱이라고 보고하지 않는다.

## 설치

휴대폰 기존 앱의 **기록 → 설정 → 앱 정보 및 라이선스 → 업데이트 → 새 버전 확인**, 다운로드·검증 후 Android의 **업데이트**를 누른다. 앱을 삭제하지 않는다. Windows 0.1.9 이상은 **⋯ → 업데이트 확인**. 로그인·SQLite·AppData·Tailscale 정보를 지우지 않는다. 설치 실패 시 오류와 기존 버전/서명을 먼저 조사한다.

## 게시 후 최종 확인

- 게시: **2026-10-12 05:54:42 UTC / 14:54:42 KST**, stable, draft=false, prerelease=false. Tag `v2.8.50`은 `bd9ed8d5b033472a7ec3e768d195db41e64dcfc4`를 가리킨다.
- [GitHub Actions 38209519439](https://github.com/ldjkorea/codex-meter-ko/actions/runs/38209519439): 해당 배포 커밋의 Android/Wear 빌드·lint·전체 회귀/현지화/폰트/소스 검사와 Windows 빌드·회귀 모두 성공. Node 20/setup-java v4 deprecation annotation은 비차단 경고로 남아 있다.
- 13개 draft 자산을 다시 내려받아 크기·모든 SHA-256·source/version을 확인한 다음 게시했다.
- 게시 후 **인증 헤더 없는 HTTPS**로 13개 파일을 모두 다시 내려받았다. 파일 크기, GitHub asset digest, 로컬 원본, 공개 `SHA256SUMS.txt` 모두 일치했다. 공개 최신 Release가 v2.8.50이며 31~49 중간 Release가 없음을 확인했다.
- Android 생산 `GitHubReleaseParser`/`UpdateChannel`로 공개 API 원문을 읽어 22개 검사 통과. 이전 v30의 stable·기존 업데이트 채널이 v50을 인식하고 동일/상위 버전에는 다운그레이드를 제안하지 않는다.
- Windows 생산 `WidgetUpdate.Parse`/`HashMatches`가 공개 0.1.10의 이름·크기·ZIP 체크섬을 인식했다. 실제 사용자 Windows 프로그램 교체·AppData 변경은 하지 않았다.
- 최종 커밋에서 다시 빌드한 Phone/Wear APK가 공개 후보와 바이트 해시가 일치했다. 공개 다운로드 APK의 인증서도 다시 확인했다. 게시 뒤 APK나 공개 자산을 교체하지 않았다.
- [휴대폰 APK](https://github.com/ldjkorea/codex-meter-ko/releases/download/v2.8.50/CodexMeter-2.8.50-ko.apk), [Wear APK](https://github.com/ldjkorea/codex-meter-ko/releases/download/v2.8.50/CodexMeter-Wear-2.8.50-ko.apk), [Windows ZIP](https://github.com/ldjkorea/codex-meter-ko/releases/download/v2.8.50/CodexMeter-WindowsWidget-0.1.10.zip), [체크섬](https://github.com/ldjkorea/codex-meter-ko/releases/download/v2.8.50/SHA256SUMS.txt).

초기 QA 과정의 위젯 배치 권한 누락, exact patch의 중복 역적용, 새 release-history 항목의 `origin` 누락을 수정했다. 화면 검사의 언어/글꼴을 Activity 생성 전에 적용하고 실제 적용값을 추가 검증했다. 기존 QA 자료를 삭제해 실패를 우회하지 않았다. 초기 실패를 최종 통과 로그로 보고하지 않고 수정 후 전체 회귀·관련 native를 재실행했다.

**판정:** BUILD READY / 공개 배포 및 기존 자동 업데이트 채널 인식 확인 완료. 기존 공개 인증서 기준 INSTALL READY. DEVICE VERIFIED는 미완료이며 사용자의 실제 설치본 인증서·데이터·갤럭시 기능은 업데이트 후 확인해야 한다.
