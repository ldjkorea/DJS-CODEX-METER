"""Stable 3.1.0 promotion contracts; no runtime credentials, devices or external requests."""
from pathlib import Path
import subprocess,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[2]
base="d41f8e4a536b235471548dc28533d304f1e1c85f"
def old(rel):return subprocess.check_output(["git","-C",str(root),"show",base+":"+rel]).decode("utf-8").replace("\r\n","\n")
app="android/app/src/main/java/dev/bennett/codexmeter/"
protected=[app+name+".java" for name in ["OAuthClient","Pkce","AuthTokens","SecureTokenStore","UsageParser","ResetCreditApi","GitHubReleaseParser","UpdateChannel","UpdateActivity","AppPreferences","WidgetOptions","WidgetRenderer","TierTheme","TierCompanionView"]]+["android/shared/src/main/java/dev/bennett/codexmeter/"+name+".java" for name in ["RpgEngine","RpgArchive","ProfileCard","KoreanUpdateTrust","WidgetTierPalette"]]
for rel in protected:assert (root/rel).read_text(encoding="utf-8")==old(rel),rel
ns="{http://schemas.android.com/apk/res/android}"
manifest=ET.parse(root/"android/app/src/main/AndroidManifest.xml").getroot();prior=ET.fromstring(old("android/app/src/main/AndroidManifest.xml"))
newapp=manifest.find("application");oldapp=prior.find("application");added={"dev.bennett.codexmeter."+name for name in ["BetaSetupActivity","BetaCatalogActivity","BetaArchiveActivity","BetaBackupActivity","BetaProfileActivity"]}
for item in list(newapp):
 if item.get(ns+"name") in added:
  assert item.tag=="activity" and item.get(ns+"exported")=="false";newapp.remove(item)
assert manifest.attrib==prior.attrib and newapp.attrib==oldapp.attrib
# XML formatting is non-semantic; compare every attribute, child, action and filter.
def tree(e):return e.tag,sorted(e.attrib.items()),[tree(c) for c in e]
assert tree(manifest)==tree(prior),"Original launcher, permission, OAuth and Wear components preserved"
source=(root/app/"UsageApi.java").read_text(encoding="utf-8");hook="                BetaGrowthStore.response(context, SubscriptionStore.accountKey(context), usageSnapshot);\n"
assert source.count(hook)==1 and source.replace(hook,"")==old(app+"UsageApi.java")
assert source.index("if (!AppPreferences.saveSnapshot")<source.index(hook)<source.index("UsageHistoryRecorder.record")
home=(root/app/"BetaHomeUi.java").read_text(encoding="utf-8")
assert "TierTheme.tier(a)" in home and "TierCompanionView" in home and "BetaGrowthStore.peek(a)" in home
assert not any(k in home for k in ["NETWORK_LOCK","getWritableDatabase",".reconcile(","BetaGrowthStore.load("])
for p in (root/app).glob("*.java"):
 text=p.read_text(encoding="utf-8")
 assert not any(k in text for k in ["TrialDemo","codexmetertrial","dev.bennett.codexmeter.trial.permission","GPT HUD BETA"]),p.name
assert "applicationId = \"dev.bennett.codexmeter\"" in (root/"android/app/build.gradle.kts").read_text(encoding="utf-8")
for module in ["app","wear"]:
 text=(root/f"android/{module}/build.gradle.kts").read_text(encoding="utf-8");assert 'versionName = "3.1.0"' in text and "versionCode = 87" in text
print("Stable promotion: preserved",len(protected),"auth/update/legacy/widget contracts; launcher/Wear/OAuth XML, fresh-save hook, cached home and production identity PASS")
