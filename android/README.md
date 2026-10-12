# Android 개발 안내

`app/`은 휴대폰, `wear/`는 Wear OS, `shared/`는 공통 모델입니다. 현재 통합 개발본의 앱·Wear는 2.8.30 / versionCode 60입니다. 내부 Phase A~D 번호는 공개 버전이 아닙니다.

JDK 21, Android SDK 36/37.0, Build Tools 36.0.0을 사용합니다. 루트 README의 빌드 명령과 docs/BUILD_SECURITY.ko.md를 참고하세요.

기존 한국어 키가 없는 환경의 Gradle Release는 unsigned입니다. `build.sh`는 기존 로컬 키가 없으면 실패하며 자동 생성하지 않습니다. 키·개인 토큰·사용자 데이터를 저장소에 넣지 마세요.

`vendor/m2`는 기존 로컬 의존성 캐시입니다. 삼성 폰트가 포함된 두 AAR는 Pretendard로 패치한 자산을 유지하며, 공개 전 `verify-publication.py`로 출처와 금지 폰트 해시 부재를 확인합니다. 원본 자동 서명 workflow 및 암호화된 release key는 이 후보에서 제외했습니다.

## 로컬 검증과 데이터 보존

- `bash run-tests.sh`: JVM/SQLite/Windows 합성 검사. Android UI·실기기 검증과 구분합니다.
- `:app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease`: 기존 로컬 키와 lint baseline을 사용합니다. baseline 확대만으로 오류를 숨기지 않습니다.
- `tests/verify-rpg-growth-native.py`: 별도 QA 패키지와 기존 에뮬레이터에서만 실행합니다. 실사용 앱을 삭제하거나 QA 자료를 사용자 DB에 넣지 않습니다.
- RPG 암호화 백업은 같은 계정의 성취·결산·수집 시작 상태를 보존합니다. 로그인 토큰과 PC 연결 비밀을 복원하는 기능이 아닙니다. 새 기기에서는 같은 계정으로 다시 로그인합니다. 일반 설정 백업의 신규 인증정보 내보내기는 차단됩니다.
- 위젯·작업현황은 Android 절전 정책에 따라 갱신이 늦어질 수 있습니다. 작업현황 화면의 15초 확인과 배경 위젯의 최선 노력 주기는 다릅니다.
- Phase D는 로컬 개발·테스트·커밋만 허용합니다. 공개 Push·Tag·Release·APK 업로드 금지. 실제 패키지·서명·versionCode를 다음 통합 QA에서 다시 검사합니다.

사용자 절차는 [RPG 백업 안내](../docs/RPG_BACKUP_GUIDE.ko.md)를 참고하세요. 과거 Release Notes의 버전별 내용은 역사 자료로 보존합니다.
