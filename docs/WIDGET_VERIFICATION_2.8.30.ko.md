# GPT HUD 2.8.30 검증 결과

## 로컬 성공

- 전체 `bash android/run-tests.sh`: 성공. 기존 로그인/OAuth·원장·리셋·가성비·RPG·백업·위젯 계산 회귀 유지. 새 위젯 상태/재렌더 검사 23개. 53개 기존 업적 정의 유지 검사 포함.
- `python android/tests/verify-localization.py`: 성공. 앱 1,655 문자열/71 배열 항목, Wear 116 문자열/13 배열 항목; 한국어/영어 일치. 보호 계약 37개 및 기존 포맷 검사 유지.
- `python android/tests/verify-rpg-growth-native.py emulator-5554 --widget30 --reuse-owned-fixture`: 50개 성공. 이전 QA v2.8.29/code59에서 3개 Provider ID·색상·순서·투명도·휘장·탭 설정 저장 후 같은 QA 패키지를 v2.8.30/code60으로 `install -r`. 기존 ID/설정 보존 확인. 최초 구버전 seed 결과는 5개 성공으로 별도 보존.
- 같은 운영 소스의 RemoteViews에서 180×70 / 250×85 / 320×180dp, 글꼴 1.0/1.5 렌더. 기본 4개/높이 확장 6개, 조회 실패·오래된 상태·작업 없음·5시간 OFF·실측 0/미확인 구분 확인. 클릭은 기존 PendingIntent 연결과 설정 Activity 진입을 확인했고 실계정 API 조회는 수행하지 않음.
- `python android/tests/verify-rpg-growth-native.py emulator-5554 --reborn30 --reuse-owned-fixture`: 133개 성공. 실제 성장 Activity·SQLite·ON/OFF·확인 취소·재시작·대표 메달·기존 칭호·보관함 보존. 320/360/411dp, 글꼴 1.0/1.5, 한국어/영어 상세 카드 클릭과 86개 합성 캡처.
- Windows 호환 검사: 278개/오프스크린 렌더 112건 성공. 새 Windows 소스·버전·실행본 교체 없음. 공개 0.1.9 ZIP SHA-256 `1376c179a53d29beb81cf8cdf3d175d54abaae445d591c740b127494c72d9c96` 유지.
- `:app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease`: 성공. 새 lint 오류 0개. 앱 경고 244개/Wear 11개, 기존 baseline의 오류 53개/경고 111개는 필터됨. baseline은 변경하지 않음. 내부 v29 앱 경고 238개 대비 새 소형 텍스트 관련 경고 등이 있으며 모든 기존 경고를 수정한 것으로 보고하지 않음.
- 두 최종 APK의 `apksigner verify --verbose --print-certs`: 성공. 휴대폰 패키지 `dev.bennett.codexmeter`, 2.8.30/code60, 기존 인증서 `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3` 유지.
- `verify-publication.py`의 두 최종 APK 검사: 성공. 공식 Pretendard/라이선스·패치 AAR·금지 폰트 해시 부재 확인.

## 검사 중 수정한 문제

- 작은 링/자동 배터리 목록에서 갱신 상태 뷰가 0×0이라 오류 안내가 보이지 않음: 실제 안내 공간과 내용 하단 여백 추가.
- 작업 헤더 포맷 문자열 오류: 포맷별 문자열 호출을 분리하고 빌드/lint 재검증.
- 큰 글꼴에서 작업 헤더 티어 문구 잘림: 고정 높이를 콘텐츠 높이로 변경 후 실제 렌더 확인.
- 설정 재저장 때 화면에 없는 기존 옵션이 기본값으로 변함: 기존 전체 옵션을 기준으로 편집 가능한 필드만 변경.
- 성장 Native QA가 비동기 갱신 중 예전 카드의 존재만 보고 클릭하는 경합: 테스트가 실제 갱신 완료까지 기다리게 변경. 운영 성장 계산/조건을 완화하지 않고 133개 재검증.
- 오래된 소스 동일성 검사: 이번 위젯 표시/상태 변경만 정확한 역변환 목록으로 허용하고 나머지 원본 비교는 유지. 역사 fixture를 덮어쓰지 않음.

## 미검증

실제 삼성 휴대폰 업데이트/로그인/API·One UI 홈/AOD/잠금화면/Now Bar·Wear 화면·PC↔폰 LAN/Tailscale 실통신·장기 Doze/배터리·사용자 실제 DB 복구는 미검증. 에뮬레이터/합성 자료의 성공을 사용자 기기 성공으로 표시하지 않음. PC 승인/입력 대기 상태는 현재 수집기에 없으므로 구현 완료라고 주장하지 않음.

## CI·서명·최종 파일

[GitHub CI 38193471824](https://github.com/ldjkorea/codex-meter-ko/actions/runs/38193471824): Android/Wear 전체 회귀·리소스·폰트·빌드/lint 및 Windows 검사 모두 성공. 기능 코드 검증 커밋은 `9b11d94`이며 이후 변경은 설치 안내/검증 문서뿐입니다.

기존 공개 v2.8.28 APK의 인증서와 새 APK 인증서가 동일함을 비교했습니다. 최종 파일:

| 파일 | 바이트 | SHA-256 |
| --- | ---: | --- |
| CodexMeter-2.8.30-ko.apk | 27,003,529 | `29e49cdf08e960f45c6c786dc3637869453073621bc5563f4fc4883f03c461d5` |
| CodexMeter-Wear-2.8.30-ko.apk | 16,139,369 | `a69ab1a7770f6de1e5a2779968aded1d61faf4164c051b2075ab871410f7ce34` |

공개 후 다운로드·체크섬 및 기존 앱 업데이트 파서의 실제 GitHub 메타데이터 검증은 최종 작업 보고로 제공합니다. 테스트 로그·개인 DB·백업·인증 정보·서명 개인키는 공개 자산에 포함하지 않습니다.
