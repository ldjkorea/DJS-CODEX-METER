from pathlib import Path
import os,subprocess,struct,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];out=root/'build/profile-tests';out.mkdir(parents=True,exist_ok=True)
jdk=Path(os.environ['JAVA_HOME'])/'bin';shared=root/'shared/src/main/java/dev/bennett/codexmeter'
# Use the preceding JVM fixtures; CI runs these before any Android Gradle build.
cp=os.pathsep.join(map(str,[out,root/'build/rpg-step2',root/'build/rpg-step1',root/'build/insight-verification/classes',root/'build/subscription-store-verification/classes',root/'build/evolution-verification',root/'build/test-libs/json-20250517.jar']))
subprocess.run([jdk/'javac','-encoding','UTF-8','-cp',cp,'-d',out,shared/'ProfileCard.java',root/'tests/ProfileCardSelfTest.java'],check=True)
subprocess.run([jdk/'java','-ea','-cp',cp,'dev.bennett.codexmeter.ProfileCardSelfTest'],check=True)
sets=[]
for folder in ['values','values-ko']:sets.append({x.attrib['name']:x.text for x in ET.parse(root/f'app/src/main/res/{folder}/profile_strings.xml').getroot()})
assert sets[0].keys()==sets[1].keys() and all(sets[0].values()) and all(sets[1].values())
paths=ET.parse(root/'app/src/main/res/xml/profile_paths.xml').getroot();assert len(paths)==1 and paths[0].tag=='cache-path' and paths[0].attrib['path']=='profile-share/'
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
provider=next(x for x in manifest.find('application').findall('provider') if x.attrib[ns+'authorities']=='${applicationId}.profile.files')
assert provider.attrib[ns+'exported']=='false' and provider.attrib[ns+'grantUriPermissions']=='true'
print('Profile bilingual resources and narrow FileProvider root passed')

def check_png(data):
    assert data[:8]==b'\x89PNG\r\n\x1a\n','PNG signature'
    at=8;chunks=[]
    while at<len(data):
        length=struct.unpack('>I',data[at:at+4])[0];kind=data[at+4:at+8]
        assert at+12+length<=len(data),'Truncated PNG'
        assert kind not in (b'tEXt',b'zTXt',b'iTXt',b'eXIf'),'Personal metadata is forbidden'
        chunks.append(kind);at+=12+length
    assert at==len(data) and chunks[-1]==b'IEND','Complete PNG'
    return chunks

# The synthetic native output is optional on developer machines, never a user file.
captures=root/'build/profile-native/captures/profile-fixtures'
if captures.exists():
    files=list(captures.glob('*.png'));assert files
    for file in files:check_png(file.read_bytes())
    original=files[0].read_bytes();injected=original[:8]+struct.pack('>I',5)+b'tEXt'+b'email'+b'\x00'*4+original[8:]
    try:check_png(injected)
    except AssertionError:pass
    else:raise AssertionError('Metadata injection must fail')
    print(f'{len(files)} synthetic PNGs contain no text/EXIF metadata; injection rejected')
