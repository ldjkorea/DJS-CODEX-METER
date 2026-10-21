# GPT HUD BETA 9.9.13 — 홈·친구 말투

## 기준과 범위

- 기준 베타 HEAD `d2fee21b96b0335dc5e1d5ec41421c89d4f3e585`(9.9.12/code990012). 사용자 승인: 최종 검토본을 베타로 업데이트·배포하고 정식은 보존한다.
- phone `dev.bennett.codexmeter.trial` /9.9.13/code990013. 기존 인증서 유지. 정식3.1.2/main·공개 APK·Wear/Windows·호환 저장소는 수정하지 않는다.
- 2026-10-21 로컬 friend-r3 검토 범위인 생산7파일을 이식했다. MainActivity는 호출 한 줄만 이동해 베타 고유 내부 권한과 OAuth scheme을 유지했다. 베타 네이티브 검증 중 발견한 이전 snapshot 작업의 플랜 혼동을 추가로 차단했다.

## 파일별 변경

| 파일 | 이유 |
|---|---|
| MainActivity / FunHome | 티어·한도/내 상태 상세 → 한마디 → 이번 주간 사용량. 중복 끝 공백 제거. 기존 비동기·스크롤 경로 유지. |
| CoachUi | 제목/본문/팁 버튼 중앙정렬,2줄 말줄임 제거, 기존 Pretendard·색상·18sp·48dp 터치 높이 유지. 팁/복사/설정 구현 보존. |
| SpicyQuotes | 전체 한도·플랜·오류·신선도에 표시 캐시 결속. 같은 상황에서는 선택ID/중복 회피 유지. 새 snapshot과 다른 과거 worker 입력을 거절해 현재 플랜에 과거 유료 문구를 게시하지 못하게 한다. |
| CoachCatalog | 실제 소수/다른 한도 양수를0으로 분류하지 않고, 누락/오류/만료/관련 한도 불완전 상태를 구분. 확인된 Plus/Pro/Pro Lite에만 유료 대체 표시. |
| 두 한국어 카탈로그 | 624개 중200수정/424유지, 신규ID0/40팁과 프롬프트·legacy mapping·숨김 기록 유지. 선택적 paidText4개, schema2/contentVersion4, 메달version1. |
| app Gradle / AppConstants | 베타 versionName9.9.13/versionCode990013만 증가. |
| android/build.sh | 베타 phone 버전/파일명 수정. 이미 settings에서 제외된 Wear 모듈 호출·산출물 참조를 제거하여 phone-only 빌드로 일치시킴. Wear 소스는 불변. |
| 새 검증과 beta workflow | 승인된10생산파일 범위와 나머지843파일의 보존을 검사. 과거9911/9912 소스 고정 검사는 파일을 남기고, 이번 요구에 해당하는 새 현재 계약으로 CI를 수행. 기존 Beta998의 콘텐츠 revision 기대값 한 곳만3→4. |

## 말투와 선택

- 관련 한도가 모두0: “야 코덱스랑 낯가리냐? 말 좀 걸어ㅋㅋ”, “코덱스는 출근했는데 사장님이 일을 안 줌.”
- 같은0이며 Plus/Pro/Pro Lite가 확인됨: “구독은 {확인된 플랜}, 사용량은 체험판. 이럴 거면 구독 왜 함?ㅋㅋ”, “한도 아끼다 구독료 기부했네ㅋㅋ 크리스마스 자선냄비인 줄.”
- 많은 사용: “야 한도 끝자락이네. 코덱스랑 장기 투숙 중이냐ㅋㅋ”.
- 미조회: “야 숫자부터 안 왔어. 눈치로 0 찍는 재주는 없어ㅋㅋ”.
- 오류: “조회가 삐끗했네. 네 작업까지 망했다는 뜻은 아냐ㅋㅋ”.

이는 선택 후보이며 매번 같은 문구를 강제하지 않는다. reset/imminent/bonus/burst/확인된 메달과 최근 중복 회피를 유지한다. 메달은 critical/missing/stale/network를 덮지 않는다. Free/미확인/임의 플랜 문자열에서는 결제를 전제하지 않는 기본 text를 쓴다. 금액·실제 결제 내역·대화 본문·업무 성과를 읽지 않는다. 편집·추가·수동 교체는 없고 OFF/기존 숨김은 보존한다. 팁 조회·복사에 XP/보상·네트워크 호출을 추가하지 않는다.

기존 `ko.practice.problem` → `tip.problem` 프롬프트: “[상황]에서 해결할 문제를 한 문장으로 정의해. 관찰한 사실과 가정을 나누고, 성공 여부를 확인할 관찰 가능한 기준3개를 제안해. 없는 정보는 질문으로 남겨.” 사용자가 ‘한 수 써먹기’에서 확인하고 복사한다.

## 실제 로컬 검증

환경 JDK21 / SDK36 / API35 x86_64 / 전용 합성 AVD `gpthudbeta9913preview`.

```text
python android/trial/run-beta-checks.py
python android/trial/verify-beta9913-source.py
python android/tests/verify-coach-friend.py
bash android/gradlew --project-dir android :app:assembleRelease :app:lintRelease --offline --console=plain
python android/tests/verify-publication.py <9.9.13 APK>
python android/trial/run-beta9913-native.py --class-name Beta9913HomeNativeTest --phase before|verify
python android/trial/run-beta9913-native.py --class-name Beta9913VoiceRaceNativeTest
python android/trial/run-beta9913-native.py --class-name Beta997AuthRecoveryNativeTest
python android/trial/run-beta9913-native.py --class-name Beta9913UpgradeInstallNativeTest --phase seed|before|verify
```

- JVM: 신규 릴리스33 / 문구·경계1348 / 주소·서명53 / 메달415 / 주간21 / 기존 콘텐츠·회고766 / 계정 lifecycle29 / 성장 보존1112 / updater140 / MXP1533 / 선택기30466 통과.
- 843개 생산파일 중 정확히 승인된10파일만 변경. 인증·DB·성장·한도표시·티어·권한·Wear/Windows 보존. 콘텐츠ID/40팁/원래 mapping 보존. 기존 코드에서0오인2건을 재현하고 수정 후 통과.
- 최종 Release/lint offline 성공.0errors/279warnings, 기존 baseline53errors/110warnings 유지. 폰트54archive·기존 서명·APK 확인 통과. 개별 native javac source17 system-module 경고1개는 남는다.
- 실제 최종 베타 전체 홈216검사: 비동기 commit/급속 최신값/세 프리셋/가로↔세로 회전/문구ID/중앙정렬/전체 문장/OFF/오류/누락/플랜 및 소수 캐시. 이전32조합711카드 검사는 카드 메서드가 같아 재사용하며 베타 전체 행렬이라고 부르지 않는다.
- 이전 Plus snapshot을 현재 Free 상태에서 실행하면 거절하지 않는 실패를 수정 전 APK에서 실제 재현했다. 수정 후 native5검사 통과.
- 실제 OAuth service·취소/재시도·PKCE/loopback·늦은 callback 차단18검사 통과. 브라우저 이동을 차단하고 가짜 응답만 사용한 합성 검사이며, 실계정 로그인 성공 증거가 아니다.
- 로컬 APK 교체 후 보존38검사: Lv30/XP271/명예15/칭호9/장착/이름/테마/코치OFF/숨김/업데이트OFF/사용량 수집OFF/암호화 합성 인증·SQLite·내보내기 유지. 공개 앱내 설치 증거와 구분한다.

## 초기 실패와 검증 한계

- 기존 Beta998SelfTest 콘텐츠 revision3 기대값에서 처음 실패했다. 이번 승인 콘텐츠 revision4 한 값만 맞췄고150개 과거 검수 metadata 등 나머지 검사는 유지해 통과했다.
- 새 AVD 초기 부팅 전에 설치를 요청해 Android package manager가 준비되지 않은 오류가 있었다. boot_completed=1 확인 후 같은 합성 기준 앱을 정상 설치했다.
- 데모가 기본적으로 다수 문구를 숨기는 fixture와 Instrumentation target Context의 이전 언어 설정 때문에 새 전체홈/direct selector 검수가 초기 실패했다. 임시 한 개 숨김 fixture를 쓰고 종료 시 원본을 복원하며, direct selector만 명시적 한국어 Context로 검사한다. 실제 홈 UI는 실제 앱 locale·MainActivity를 검사했다. 실패 로그를 보존했다.
- 그 과정에서 별도 재현한 과거 snapshot worker 문제는 실제 생산 코드 결함이므로 베타에서 수정했다. 실패를 테스트 삭제나 전체 fixture 갱신으로 숨기지 않았다.
- 실물폰/실계정 OAuth·실제 결제·실사용 DB 복원/모든 OEM 장기실행/TalkBack 음성/Wear·Windows 실연동/전체 과거 고정 소스 fixture는 미검증이다. 추가AI/API/분석서버/키/권한/의존성/Drive 업로드 없음.

## 배포 경로와 최종 증거

베타 `beta-v9.9.13`, prerelease/latest=false로만 게시한다. 정식 main·latest v3.1.2와 기존 릴리스 자산은 전후 비교한다. 공개 후 별도 소유 AVD `gpthudbeta9913public`에서 기존 공개9.9.12 → 앱 발견/다운로드 → Android 설치 동의 →9.9.13 및 데이터 보존/설치 해시를 검증한다.

최종 배포 상태·원격 source commit·CI·공개/설치 SHA256·실행 결과는 `SOURCE/delivery/beta-9.9.13/release-20261022/VERIFICATION.ko.md`와 `release-receipt.json`에 기록한다. 이 문서의 로컬 교체 결과를 공개 업데이트 E2E로 대체하지 않는다. 이전 로컬 정식 패키지 friend-r3는 베타 배포 파일이 아니다.
