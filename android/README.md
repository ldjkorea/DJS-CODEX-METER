# Android 개발 안내

`app/`은 휴대폰, `wear/`는 Wear OS, `shared/`는 공통 모델입니다. 이 베타 브랜치의 phone 앱은 9.9.6 / versionCode990006이고 Wear는 기존3.0.0 /85를 유지합니다. 검증 결과와 정리 내역은 `../docs/GPT_HUD_3.0.0.ko.md`에 기록합니다.

JDK 21, Android SDK 36/37.0, Build Tools 36.0.0을 사용합니다. 루트 README의 빌드 명령과 docs/BUILD_SECURITY.ko.md를 참고하세요.

기존 한국어 키가 없는 환경의 Gradle Release는 unsigned입니다. `build.sh`는 기존 로컬 키가 없으면 실패하며 자동 생성하지 않습니다. 키·개인 토큰·사용자 데이터를 저장소에 넣지 마세요.

`vendor/m2`는 기존 로컬 의존성 캐시입니다. 삼성 폰트가 포함된 두 AAR는 Pretendard로 패치한 자산을 유지하며, 공개 전 `verify-publication.py`로 출처와 금지 폰트 해시 부재를 확인합니다. 원본 자동 서명 workflow 및 암호화된 release key는 이 후보에서 제외했습니다.

## 로컬 검증과 데이터 보존

- `bash run-tests.sh`: JVM/SQLite/Windows 합성 검사. Android UI·실기기 검증과 구분합니다.
- `:app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease`: 기존 로컬 키와 lint baseline을 사용합니다. baseline 확대만으로 오류를 숨기지 않습니다.
- `tests/verify-rpg-growth-native.py`: 별도 QA 패키지와 기존 에뮬레이터에서만 실행합니다. 실사용 앱을 삭제하거나 QA 자료를 사용자 DB에 넣지 않습니다.
- RPG 암호화 백업은 같은 계정의 성취·결산·수집 시작 상태를 보존합니다. 로그인 토큰과 PC 연결 비밀을 복원하는 기능이 아닙니다. 새 기기에서는 같은 계정으로 다시 로그인합니다. 일반 설정 백업의 신규 인증정보 내보내기는 차단됩니다.
- 위젯·작업현황은 Android 절전 정책에 따라 갱신이 늦어질 수 있습니다. 작업현황 화면의 15초 확인과 배경 위젯의 최선 노력 주기는 다릅니다.
- 3.0.0은 로컬 통합판입니다. 공개 Push·Tag·Release·APK 업로드는 별도 승인 후 수행합니다. 실제 기기 설치 여부와 로컬 검증 결과를 구분합니다.

사용자 절차는 [RPG 백업 안내](../docs/RPG_BACKUP_GUIDE.ko.md)를 참고하세요. 과거 Release Notes의 버전별 내용은 역사 자료로 보존합니다.
