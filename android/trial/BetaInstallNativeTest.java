package dev.bennett.codexmeter;
import android.app.*;import android.content.*;import android.os.*;import android.view.accessibility.AccessibilityNodeInfo;

/** Own emulator only: verify actual PackageInstaller confirmation, never silently install. */
public final class BetaInstallNativeTest extends Instrumentation {
    int checks;void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
    static boolean confirmation(AccessibilityNodeInfo n){if(n==null)return false;String text=n.getText()==null?"":n.getText().toString();if(text.equals("Update")||text.equals("업데이트")||text.equals("Install")||text.equals("설치"))return true;for(int i=0;i<n.getChildCount();i++)if(confirmation(n.getChild(i)))return true;return false;}
    boolean full;
    @Override public void onCreate(Bundle a){super.onCreate(a);full="installer-full".equals(a.getString("phase"));start();}
    static AccessibilityNodeInfo action(AccessibilityNodeInfo n){if(n==null)return null;String text=n.getText()==null?"":n.getText().toString();if(text.equals("Update")||text.equals("업데이트")||text.equals("Install")||text.equals("설치"))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=action(n.getChild(i));if(r!=null)return r;}return null;}
    @Override public void onStart(){Context c=getTargetContext();Bundle result=new Bundle();int code=0;UpdateActivity screen=null;int session=-1;
        try{
            check(c.getPackageName().equals("dev.bennett.codexmeter.trial")&&Build.MODEL.contains("sdk"),"Owned beta emulator");check(SecureTokenStore.load(c)==null||TrialDemo.active(c),"No real account fixture");
            screen=(UpdateActivity)startActivitySync(new Intent(c,UpdateActivity.class).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(1400);waitForIdleSync();
            // Shell privilege is confined to this instrumentation on the owned synthetic emulator.
            try(android.os.ParcelFileDescriptor p=getUiAutomation().executeShellCommand("appops set dev.bennett.codexmeter.trial REQUEST_INSTALL_PACKAGES allow");java.io.FileInputStream in=new java.io.FileInputStream(p.getFileDescriptor())){in.readAllBytes();}
            check(c.getPackageManager().canRequestPackageInstalls(),"Owned fixture granted installer source permission");
            String installed=UpdatePreferences.installedVersion(c);String target=full?"9.9.3":"9.9.2";
            GitHubRelease release=GitHubReleaseParser.findVersion(ReleaseUpdateClient.check(c),target);check(release!=null,"Existing public higher beta selected");
            UpdateInstaller.PreparedUpdate update=UpdateInstaller.prepare(c,release,null);check(update.versionCode==(full?990003:990002),"Actual public APK verified");
            UpdatePreferences.setInstallError(c,"");session=UpdateInstaller.commit(c,update);
            AccessibilityNodeInfo root=null;boolean installer=false;
            for(int i=0;i<100;i++){Thread.sleep(150);root=getUiAutomation().getRootInActiveWindow();if(root!=null&&root.getPackageName()!=null&&root.getPackageName().toString().contains("packageinstaller")&&confirmation(root)){installer=true;break;}}
            check(installer,"Actual Android update confirmation reached foreground");
            check(UpdatePreferences.installedVersion(c).equals(installed),"No install without explicit Android confirmation");
            check(UpdatePreferences.installError(c).isEmpty(),"Pending user action had no receiver error");
            Thread.sleep(1200);java.io.File file=new java.io.File(c.getExternalFilesDir(null),"android-installer.png");try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
            if(full){getTargetContext().getSharedPreferences("trial_install_proof",0).edit().putString("before",installed).putInt("session",session).commit();AccessibilityNodeInfo button=action(getUiAutomation().getRootInActiveWindow());check(button!=null&&button.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Owned emulator confirms actual Android install");session=-1;Thread.sleep(10000);}
            result.putString("stream","Beta installer: "+checks+" checks passed; public APK prepared, PackageInstaller committed, actual Android confirmation visible, no silent install.\n");code=-1;
        }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e)+"\nInstaller error: "+DiagnosticSanitizer.redact(UpdatePreferences.installError(c)));}
        finally{if(session>=0)try{c.getPackageManager().getPackageInstaller().abandonSession(session);}catch(Exception ignored){}if(screen!=null){UpdateActivity a=screen;runOnMainSync(a::finish);}}
        finish(code,result);
    }
}
