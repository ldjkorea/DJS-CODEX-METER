# GPT HUD 2.9.3 언어 선택 및 APK 경량화

2026-10-13. 공개 기준은 Android/Wear 2.9.2, versionCode 83 (`v2.9.2`)입니다. 이번 변경은 Android/Wear 2.9.3, versionCode 84로 올립니다.

## 변경

- 새 설치는 최초 설정 화면에서 한국어 또는 English 중 하나를 선택합니다. 선택은 즉시 저장·적용되며 같은 선택 안내를 다시 띄우지 않습니다.
- 기존 사용자는 기존 설정을 유지합니다. 설정의 화면·위젯 영역에 앱 언어 선택을 추가했습니다.
- Android 앱 언어 설정에는 한국어와 영어만 등록했습니다. 사용하지 않는 의존 라이브러리 번역 리소스를 APK에서 제외하되 다른 밀도 리소스와 기본/한국어 문자열은 유지합니다.
- AndroidX 앱 언어 저장을 사용해 지원 OS에서는 시스템 앱 언어 설정과 연동하고 구형 지원 Android에서도 선택값을 저장합니다.
- OAuth, 원장·RPG DB, 위젯 ID, Windows 위젯은 변경하지 않았습니다.

## 검증

- 전체 `android/run-tests.sh`: 성공. 버전 가드와 offline release catalog도 2.9.3 및 63개 이력을 확인합니다.
- Android 격리 에뮬레이터 `emulator-5560`: 새 설치 선택, 한 번만 안내, 한국어/영어 2개 선택지, 즉시 영어 적용, 설정 언어 라벨 등을 포함한 **10 checks PASS**. 실사용 계정이나 실제 사용자 데이터는 사용하지 않았습니다.
- `:app:assembleRelease :app:lintRelease :wear:assembleRelease`: 성공. lint baseline에 기존 이슈가 있으며 본 변경은 경고 목록을 확장하지 않았습니다.
- Phone APK `dev.bennett.codexmeter`, version 2.9.3/code84. 공개 2.9.2와 패키지 ID 및 서명 인증서 SHA-256이 같습니다. versionCode가 증가해 기존 앱 위 업데이트 조건을 충족합니다. 실물 휴대폰에 설치하지는 않았습니다.
- Phone APK 크기: 2.9.2 **27,077,457 bytes**, 2.9.3 **24,617,745 bytes**. **2,459,712 bytes (약 9.09%) 감소**.
- 기존 Wear 앱은 코드 변경 없이 2.9.3/code84로 다시 빌드했습니다. Windows 위젯 0.1.10은 변경하지 않았습니다.

## 제한

실제 갤럭시에서의 앱 언어 시스템 설정, 제조사 런처·AOD·Now Bar, 실제 설치본의 데이터 보존은 아직 실기기 검증하지 않았습니다. 자동 업데이트는 기존 공개 Release 확인 주기를 따르며 Android가 설치 승인을 요구합니다. 조용한 자동 설치는 하지 않습니다.
