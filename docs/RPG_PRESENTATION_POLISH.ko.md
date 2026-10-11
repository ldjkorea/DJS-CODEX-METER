# 성장·업적 표현 개선 — 로컬 검증

기준: v2.8.27 / versionCode 57. 이번 작업에서 공개 버전·업데이트 채널은 변경하지 않았다.

## 변경

- 칭호: 홈·성장·보관함·미리보기에 굵은 Pretendard, 티어색, 테두리와 여백 적용.
- 대표 메달: 홈·성장·도감에 이름 표시, 카드 사이 10dp, 문양 아래 12dp 여백. 큰 글꼴에서는 세로 배치.
- 도감: 문양과 제목 간격 14dp, 이름 대비·굵기 강화, 상태 문구 여백 추가.
- 업적 53개: 기존 ID를 키로 별도 한·영 표시 이름과 짧은 설명 추가. 이전 이름도 검색 가능. 카탈로그의 조건·보상·ID는 수정하지 않음.
- 메달: 기존 고유 문양을 보존하면서 반투명 금속 바탕과 안쪽 테두리 추가.
- 세계 티어: 지구·궤도·별·월계관 벡터 문장. 대한민국과 홈은 삼족오 유지. 평가·최고 등급 데이터 변경 없음.
- 한도 상세: 일반 물결 막대를 티어색과 연동. 급증 경고색 유지.
- Tibo: 고정 인용임을 명시하고 X 프로필의 최신 글로 이동하는 버튼 추가. 자동 피드 수집은 구현하지 않음.

## 검증

- `python android/tests/verify-medal-collection.py`: 기존 5,523개 + 새 표시 데이터 286개 검사 통과. 기존 카탈로그 및 원장·인증 등 보호 파일 비교 통과.
- `python android/tests/verify-localization.py`: 한·영 리소스 및 보호 대상 37개 파일 검사 통과. 새 표시 전용 클래스는 과거 upstream에 존재하지 않아 기존 제외 목록에 명시하고 별도 검사로 검증.
- `python android/tests/verify-rpg-growth-native.py emulator-5554 --medals --presentation --reuse-owned-fixture`: 780개 통과. 격리 QA 앱·가상 계정만 사용.
- Android Debug 빌드·lint 및 일반 패키지 Release 빌드·lint 통과. Release lint: 0 errors, 225 warnings. 기존 baseline으로 53 errors, 111 warnings가 제외되어 경고 없는 프로젝트를 의미하지 않음.
- 기존 서명키로 만든 로컬 Release APK 서명 검증 통과. 실제 휴대폰에는 설치하지 않음.
- 원장·XP·달성 조건·획득일·계정 저장 엔진 변경 없음. 가상 계정에서 선택·취소·재진입·계정 분리·XP 및 획득일 보존을 검사. 개인 휴대폰 DB에 대한 검증은 아님.

## 화면 증거 및 수정 중 발견 사항

`android/build/presentation-native/captures/rpg3-fixtures/`에 Android View 렌더링 캡처를 보관한다. 320/411/700dp, 글꼴 1.0/1.5, 도감 라이트·다크 및 한국어 이름을 검사했다. 별도 성장·홈·세계 문장·한도색 합성 장면과 Tibo 영역 캡처는 영어다. 실제 삼성 런처 화면이 아니다.

- 불투명 메달 바탕이 알파 기반 문양 구분 검사를 가려 반투명으로 수정. 53개 고유 문양 검사 재통과.
- 기존 UI 테스트가 접힌 필터를 조작하던 문제를 실제 펼치기 버튼을 누르는 순서로 수정. 필터 조건은 완화하지 않음.
- QA 사진에서 직접 바꾼 목록 상태와 선택기 표시가 달라지는 문제를 테스트 캡처의 상태 동기화로 수정.

## 빌드 파일

로컬 확인용: `android/app/build/outputs/apk/release/app-release.apk`

- 패키지: `dev.bennett.codexmeter`
- 버전: `2.8.27 / 57` (새 정식 배포본이 아님)
- APK SHA-256: `20d0e86a6d805f22f60cc2e894d7a3bd37c9d1b7f7da617b5d8a74557f665dd2`
- 서명 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`

공개 배포 시에는 새 버전·versionCode를 부여하고 최종 산출물과 공개 업데이트 경로를 다시 검증해야 한다.
