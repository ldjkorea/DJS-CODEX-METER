# APK 용량 최소 변경 최적화 — 2026-10-13

기준: 공개 2.9.2/code83. 사용자 요청에 따라 사용량을 아끼기 위해 용량 원인 조사와 작은 변경에 한정했다. 공개 배포 및 버전 변경은 하지 않았다.

## 원인과 변경

- 기준 APK 27,077,457 bytes: DEX 압축 합계 10,623,250 bytes, 리소스 색인 5,019,956 bytes, 글꼴 약 6.36 MB. 사용자 DB가 포함되어 커진 것이 아니다.
- 앱 자체 언어는 기본 영어와 한국어다. Phone Gradle의 resourceConfigurations를 en/ko로 제한하여 의존성의 미지원 언어 번역을 제외했다. 다른 시스템 언어에서는 기본 영어로 대체된다. 영어 지역별 의존성 문구도 기본 영어로 대체될 수 있다.
- 코드 축소/난독화, 글꼴 교체, 이미지 재압축, 데이터 변경은 적용하지 않았다.
- sparse resource encoding도 시도했으나 Gradle APK 크기가 같아 최종 변경에서 제외했다.
- 결과 24,616,521 bytes: **2,460,936 bytes / 9.09% 절감**. 리소스 색인 2,561,340 bytes.

## 검증

- Phone Release 빌드 성공. 격리 QA Release 빌드 성공.
- `verify-apk-language-trim.py`로 원본과 후보 비교: 리소스 ID 전체 유지, 유지되는 값 15,650개 동일, 코드/Manifest/자산 등 ZIP 항목 3,206개 동일. 제거되는 28,795개 값은 언어별 의존성 항목이며 기본/일반 영어/한국어 값은 유지.
- `verify-publication.py`: Pretendard 4종, 라이선스, 금지 폰트 부재 검사 통과.
- `apksigner verify --print-certs`: 기존 인증서 SHA-256 `6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3` 일치.
- 기존 소유 합성 에뮬레이터 emulator-5560에서 `verify-rpg-growth-native.py --startup-lock --reuse-owned-fixture`: 14 checks passed, 캐시 읽기 54 ms, 네트워크 잠금 상황의 홈 진입 1876 ms, 메인 루프 응답 유지. 홈 캡처 확인. 이 수치는 성능 향상률 비교 근거가 아니다.
- 전체 회귀/lint/실제 갤럭시/OEM 위젯은 이번 소규모 작업에서 재실행하지 않았다. 공개 배포 전 최종 게이트 필요.

로컬 후보: `android/build/apk-size-audit/GPT-HUD-2.9.2-size-candidate.apk`.

SHA-256: `0f24e2b6000471d7400c9ae667521635de36768f772afffc707dd3353d7e904c`.

비교 원시 결과 및 빌드/native 로그는 `android/build/apk-size-audit/`에 보관한다. 이전 공개 APK와 docs/emblem-concepts는 보존했다. 다음에 더 크게 줄이려면 R8 및 리소스 제거를 별도 후보에서 검증해야 하며 현재 단계에서 안전성이나 절감률을 보장하지 않는다.
