# 내부 v2.9.1 — 개인정보 보호형 프로필 공유

2026-10-13. 안정화 자동 게이트 통과 뒤 추가했다. 공개 출시 버전은 변경하지 않았으며 APK 업로드·실제 친구 전송을 수행하지 않았다.

## 구현

성장 화면의 **프로필 공유** → 카드 미리보기 → **이미지 저장** 또는 **이미지 공유**. 홈/기록/성장 탐색 구조는 그대로다.

- 익명 기본값, 선택 닉네임 24 Unicode code point, 생성 날짜 기본 OFF.
- 홈과 같은 공통 티어 결과, 저널 XP 기반 레벨, 선택한 획득 메달 최대 3개, 보유한 장착 칭호만 사용한다. 기록 부족은 임의 티어/레벨로 채우지 않는다.
- 기존 휘장/메달/Pretendard로 900×1120 PNG를 만든다. 라이트/다크, 미획득/미장착 상태를 처리한다.
- Preview/PNG 작업은 로컬 worker, 입력 변경 250ms debounce. generation 및 계정 검사로 이전 콜백을 차단한다.
- 저장은 Android CreateDocument로 사용자가 고른 URI에 쓴다. 취소/파일 IO 실패를 표시한다. 외부 문서 provider IO 동안 전역 사용량 NETWORK_LOCK을 잡지 않는다.
- 공유는 명시적 버튼 → UUID 파일 → 좁은 FileProvider 읽기 URI → Android Sharesheet다. 자동 수신자 선택/전송이 없다.
- TalkBack 미리보기 설명에 티어/레벨/메달/칭호와 게임형 평가 고지를 넣고, 버튼 터치 높이를 확보했다.

## 정보와 데이터 보호

ProfileCard 객체는 허용 목록만 가진다. 계정 ID·이메일·인증/페어링 비밀정보·프로젝트 이름·경로·결제·원본 로그·프롬프트는 포함하지 않는다. 원본 JSONObject를 보관하거나 변경하지 않는다. 명시적으로 입력한 닉네임은 공유되는 이미지 내용이므로 사용자가 개인정보를 넣지 않도록 선택해야 한다.

Activity/provider는 private이고 FileProvider의 단일 root는 `cache/profile-share/`다. 계정 문자열을 파일명/URI에 넣지 않는다. DB URI는 거부한다. PNG text/EXIF 메타데이터를 추가하지 않으며 생성된 합성 이미지의 실제 PNG chunk를 검사했다. 저장된 이미지나 이미 다른 앱에 넘긴 카드는 회수할 수 없다.

캐시의 24시간 지난 파일은 다음 공유 시 정리한다. 즉시 만료/철회나 친구 계정 동기화는 제공하지 않는다. OS 캐시 정리로 오래된 URI가 사라질 수 있다. 회전 중 진행하던 문서 저장의 PNG payload는 영속화하지 않아 재시도가 필요할 수 있다.

SQLite 스키마, RPG IDs/판정/XP/칭호 장착 정책, OAuth/API, LAN/Wear/Windows 프로토콜, 서명은 변경하지 않았다. 미리보기·저장·공유 메뉴의 실패 뒤 계정 저널이 변하지 않는지 native로 확인했다.

## 검증

| 검사 | 결과 | 범위 |
|---|---|---|
| ProfileCardSelfTest | 14 PASS | 선택/잠금/최대3/Unicode/불변성/정보 허용 목록/unknown |
| ProfileShareNativeTest | 30 PASS | 실제 Activity/Bitmap/SQLite/FileProvider/저장 stream/취소/실패/계정 교체/chooser 가로채기 |
| PNG 메타데이터 | 10개 PASS | 실제 합성 PNG의 text/EXIF 부재, metadata 주입 검출 |
| 화면 | 실제 렌더 확인 | 320/360/411dp, 한국어 light 320dp·1.5배, 영어 dark 411dp·2배 |
| 리소스 | PASS | 신규 14키 한국어/영어, 기존 전체 현지화 계약 |
| 최종 전체 회귀/Release/lint | 통합 검증 문서 참조 | Phone/Wear 및 기존 생산 원장/RPG/Windows 검증 |

에뮬레이터의 실제 배포 클래스와 동일한 production 경로를 사용하되 패키지 ID만 격리했다. 실계정/production 사용자 DB에 가상 데이터를 넣지 않았다. 생성 화면은 `android/build/profile-native/captures/profile-fixtures/`에 남긴다. `tier-example.png`의 Diamond 등급은 디자인 확인용 명시적 합성 상태다.

실제 삼성·Wear, Android OS 여러 버전, 시스템 SAF chooser의 실제 사용자 파일 선택, Quick Share/카카오톡 수신, 실제 회전·프로세스 죽음 중 파일 선택: **NOT VERIFIED**. 공유 메뉴를 가로채 취소했으므로 실제 친구에게 전송했다고 보고하지 않는다.

## 변경 파일

- shared/ProfileCard.java: 계정 식별자 없는 허용 목록/표시 검증.
- app/ProfileCardRenderer.java: PNG 및 스크린리더 설명.
- app/ProfileShareActivity.java: worker/미리보기/SAF/명시적 공유/계정 경계.
- RpgGrowthUi.java, AndroidManifest.xml: 성장 진입 버튼/private Activity 및 provider.
- values/profile_strings.xml, values-ko/profile_strings.xml, xml/profile_paths.xml: 현지화/좁은 경로.
- ProfileCardSelfTest, ProfileShareNativeTest, verify-profile-share.py, run-tests.sh, native runner 및 review adapter: 독립 회귀/실제 렌더/기존 보존 계약.

판정: 로컬 기능 구현 및 자동/에뮬레이터 검증 완료. 공개 배포는 승인·정식 버전 정합성·실기기 QA 범위를 확정한 뒤 판단한다.
