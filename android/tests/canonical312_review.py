"""User-approved DJS-GPT-HUD migration; exact version/path deltas, no identity reset."""
import hashlib

SOURCE_PINS = {
 'android/app/src/main/java/dev/bennett/codexmeter/GitHubReleaseParser.java': 'bfde14592173befad23b1b667d11f613abb4228ab3141d1e26f266c42bd3fa02',
 'android/app/src/main/java/dev/bennett/codexmeter/GitHubReleaseSource.java': 'b8908b85d45bbbc2da2be3e5281ce195d75bdc931475317a7327f889374f82d2',
 'android/shared/src/main/java/dev/bennett/codexmeter/KoreanUpdateTrust.java': '7affd908e5d0bb752c4d949347391be79650dfc64cd54b9bba9fe00133d31f3a',
}

def reviewed_pin(path, raw):
 assert path in SOURCE_PINS
 assert hashlib.sha256(raw.replace(b'\r\n',b'\n')).hexdigest()==SOURCE_PINS[path], ('Unreviewed repository migration',path)

def reverse_version_delta(path, text):
 if path=='android/app/build.gradle.kts':
  assert text.count('versionCode = 89')==1 and text.count('versionName = "3.1.2"')==1
  return text.replace('versionCode = 89','versionCode = 88').replace('versionName = "3.1.2"','versionName = "3.1.1"').replace('ldjkorea/DJS-GPT-HUD/releases?per_page=30','ldjkorea/codex-meter-ko/releases?per_page=30')
 if path.endswith('/AppConstants.java'):
  assert text.count('VERSION_CODE = 89')==1 and text.count('VERSION_NAME = "3.1.2"')==1
  return text.replace('VERSION_CODE = 89','VERSION_CODE = 88').replace('3.1.2','3.1.1')
 return text

def windows_delta(path, text):
 if path=='windows-widget/README.ko.md':return '# GPT HUD Windows Widget 3.1.1\n\nDJS-GPT-HUD 저장소 주소 전환 버전입니다. 기존 3.1.0 이하는 새 주소를 직접 신뢰하지 못하므로 첫 업데이트는 공식 ZIP을 수동으로 풀어 실행합니다. 기존 AppData·로그인·설정은 삭제하지 마세요. 새 위젯은 정식 저장소와 두 과거 이름만 허용하며, Windows 실행 파일은 Authenticode 미서명입니다. 실제 계정·기기 연결은 이번 주소 전환 검증 범위 밖입니다.\n\n## 이전 버전 안내\n\n'+text
 if path.endswith('/WidgetUpdate.cs'):
  return text.replace('ldjkorea/codex-meter-ko/releases/latest','ldjkorea/DJS-GPT-HUD/releases/latest').replace('ldjkorea/codex-meter-ko/releases/download/','ldjkorea/(?:DJS-GPT-HUD|DJS-CODEX-METER|codex-meter-ko)/releases/download/')
 if path.endswith('/Widget.cs') or path.endswith('/AssemblyInfo.cs'):return text.replace('3.1.0','3.1.1')
 return text
