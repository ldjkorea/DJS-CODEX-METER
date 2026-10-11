# GPT HUD 2.8.28 공개 준비 검증

- 버전 2.8.28 / versionCode 58, 기존 패키지 `dev.bennett.codexmeter` 유지.
- 전체 `android/run-tests.sh` 성공. 새 표시용 클래스의 테스트 컴파일 목록 누락을 수정하고 전체 검사를 다시 통과했다. 릴리스 기록은 이전 55개에 새 항목을 더한 56개이며 과거 기록을 보존했다.
- `verify-localization.py`: 영어·한국어 리소스, 기존 보호 파일 37개 검사 성공.
- 업적 도감 5,523개 및 표시 문구 286개 검사 성공. 기존 53개 업적 ID·조건·XP 정의와 보호한 원장·인증·위젯 파일은 변경하지 않았다.
- `verify-rpg-growth-native.py emulator-5554 --medals --presentation --reuse-owned-fixture`: 분리된 QA 패키지·합성 계정으로 780개 검사 성공. 앱 재설치 삭제 없이 QA 패키지를 업데이트했다. 실제 삼성 휴대폰 데이터는 접근하지 않았다.
- README 사진은 이번 버전 QA 앱의 성장·업적 도감·칭호 보관함 화면이다. 영어 성장/도감과 한국어 보관함을 명시했고 옛 사진은 이력을 위해 삭제하지 않았다.
- `:app:assembleRelease :wear:assembleRelease :app:lintRelease :wear:lintRelease` 성공. 휴대폰 lint 0 errors/225 warnings, 기존 baseline으로 53 errors/111 warnings 제외. Wear lint 0 errors/11 warnings. 경고가 모두 해결된 상태는 아니다.
- `verify-publication.py <phone> <wear>`: Pretendard·패치 의존성·라이선스 및 금지 폰트 부재 검사 성공.
- APK 인증서 SHA-256: `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3`. 실제 공개 2.8.27 APK를 내려받아 동일 인증서와 새 버전 코드 증가를 비교했다.
- 휴대폰 APK SHA-256: `9ffd27d8e90c1842fcb67bd86f2ca03e0fd01346e676f9166c3058a30930c6ec`.
- Wear APK SHA-256: `dfd676020c090a479a7dadbb1f212cde1d78a835fbce5d8c27c20714b9d5f44f`.
- Windows 위젯은 기존 공개 0.1.9 ZIP을 재사용하며 기존 체크섬 `1376c179a53d29beb81cf8cdf3d175d54abaae445d591c740b127494c72d9c96`을 확인했다. 최신 릴리스 조회 호환성을 위해 새 릴리스에도 같은 파일을 포함한다. Windows 빌드·278개 검사·112개 화면 렌더 검사 성공; 실제 PC 로그인·Tailscale 연결을 변경하지 않았다.

이번 단계에서 실제 휴대폰 설치·로그인·Samsung 위젯·장시간 절전은 검증하지 않았다. 자동 업데이트는 기존 검사·다운로드 흐름을 사용하며 Android 설치 승인은 사용자에게 필요하다. Tibo 문구는 저장된 인용이고 최신 글 버튼은 X 프로필을 연다. 자동 X 수집 기능을 추가한 것은 아니다.

공개 릴리스는 준비 파일 업로드 후 다운로드·체크섬·기존 업데이트 파서 인식 여부를 별도로 확인한다. 개인 DB·서명 개인키·인증 정보는 배포 파일에 포함하지 않는다.
