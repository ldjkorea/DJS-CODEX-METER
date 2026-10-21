# DJS-GPT-HUD 저장소·업데이트 전환

## 승인과 범위

2026-10-21 사용자의 “DJS-GPT-HUD 로 전체 변경하고 필요한 절차 수행”에 따라 저장소 이름·활성 API·배포 참조를 변경한다. 기존 이름 변경/신뢰 경로 확대 금지는 이 구체적 승인 범위에서 대체된다. 저장소 ID1411531926과 소유자ldjkorea는 동일하며 과거 태그·릴리스·자산을 보존한다.

- 정식: 3.1.2/code89/dev.bennett.codexmeter, 기준HEAD4da02d390e7f82c3b29352a28f45a410c5528014, 기존 공개3.1.1 기준6638a50b3771d717bad9c6704f6c9924bda418fa.
- 베타: 9.9.12/code990012/dev.bennett.codexmeter.trial, 별도 branch와prerelease. 기준5a2d7b77c23683435f45c1d4f13eb0b35e4f4fe9.
- Windows: 3.1.1. Wear: 기존3.1.0 바이너리 재사용. APK 기술용 파일명·패키지·OAuth callback·서명키·DB·운영 디렉터리를 바꾸지 않는다.

## 원인과 수정

GitHub 저장소 이름 변경 후 API redirect는 성공해도 구버전 파서는 릴리스 URL의 저장소 이름 불일치로 목록을 모두 거절한다. 업데이트 캐시도 빈 결과로 교체할 수 있었다. 새로운 앱은 DJS-GPT-HUD와 정확한 두 과거 이름 DJS-CODEX-METER/codex-meter-ko만 인식한다. 한 릴리스의 페이지·APK·체크섬 주소는 같은 root/tag여야 한다. 임의 같은 소유자 저장소·외부 도메인·혼합 root·userinfo·인코딩 경로·잘못된 인증서·다운그레이드는 거절한다. 다운로드/설치 보안·계정·성장 엔진은 유지한다.

**기존 설치 앱에 새 검증 코드를 원격 주입할 수 없다. 정식·베타·Windows 모두 최초 한 번 공식 APK/ZIP 수동 업데이트가 필요하다.** 삭제/초기화를 안내하지 않는다. 새 버전에서는 기존 자동 확인 설정·주기를 유지하고, Android 설치는 사용자 동의를 따른다.

## 검증 계약

- 현재 정식 소스는 기존 배포본의 생산파일841개를 대조하며 정확한11개 변경과 새 오류 리소스2개만 허용한다. 베타도 동일 방식으로6개 변경/리소스2개만 허용한다. 텍스트의 CRLF만 정규화한다. 앱·공유·Wear·Windows 소스/자산·빌드 설정을 포함한다.
- 승인된 새 API 주소를 ParserSelfTest의 두 기대값에 반영한다. 과거 응답 fixture·고정 커밋·과거 릴리스 내역은 변경하지 않는다. 새 부정검사는 현재 버전 다음 patch를 계산한다.
- 원래 전체 historical source snapshot 검사는 UsageApi 등의 과거 UI/기록 hook 불일치가 남아 있으므로 통과라고 보고하지 않는다. 수동 historical-source-audit workflow를 유지하며 무시/continue-on-error를 넣지 않는다. 배포 CI는 실제 현재 기능/JVM·841파일 보존·리소스·응답 경계·설치 신뢰·공개 소스 검사를 사용한다.
- 새 버전의 현재 피드 선택과 구버전의 실패를 별도로 재현한다. 구버전 선택 실패를 자동 업데이트 성공으로 바꾸어 보고하지 않는다. 이전512KiB와 현재2MiB 한도는 유지하며, 공개 목록 크기는 게시 시 다시 측정한다.

## 로컬 결과와 한계

정식3.1.2 release/lint 성공:0 errors/280 warnings, 기존baseline53errors/111warnings 제외. Windows291assertions/112offscreen renders 및 RPG metadata14통과. 베타 실제 성장/콘텐츠/업데이트 JVM과 정확한 생산파일 보존 검사를 수행한다. 최종 CI·APK 해시·인증서·익명 자산 검증 결과는 배포 폴더의 단일 VERIFICATION.ko.md와 release-receipt.json에 기록한다.

이번 버전의 새 에뮬레이터 시스템 설치·실물폰·실계정 OAuth·Wear/Windows 실연결·사용자 데이터 복원은 미검증이다. 예전 설치 성공을 이번 성공으로 재사용하지 않는다. 기존 파일 동일성은 실제 실계정 이관 성공을 뜻하지 않는다. Windows Authenticode 미서명. 사용자 폰/실사용 데이터·새 키·Drive 업로드·별도 분석서버 변경 없음.
