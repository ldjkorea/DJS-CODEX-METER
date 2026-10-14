package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Exercise the actual service/receiver and loopback callback without a real OAuth account. */
public final class BetaAuthNativeTest extends Instrumentation {
    String phase;int checks;
    void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){super.onCreate(args);phase=args.getString("phase");start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;Context c=getTargetContext();MainActivity home=null;ActivityMonitor monitor=null;
        try {
            check(c.getPackageName().equals("dev.bennett.codexmeter.trial")&&Build.MODEL.contains("sdk"),"Synthetic beta emulator only");
            check(SecureTokenStore.load(c)==null||TrialDemo.active(c),"Never touch a real signed-in account");
            check(c.checkSelfPermission(AppConstants.INTERNAL_PERMISSION)==PackageManager.PERMISSION_GRANTED,"Own beta internal permission granted");
            check(c.checkSelfPermission("dev.bennett.codexmeter.permission.INTERNAL")==PackageManager.PERMISSION_DENIED,"No regular-app internal permission");
            check(AppConstants.APP_LINK.equals("codexmetertrial://auth/complete"),"Separate beta browser return link");
            Intent callback=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(AppConstants.APP_LINK)).addCategory(Intent.CATEGORY_BROWSABLE);
            check(c.getPackageManager().resolveActivity(callback,PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName.equals(c.getPackageName()),"Browser return resolves to beta");
            TrialDemo.leaveDemo(c);
            IntentFilter browserFilter=new IntentFilter(Intent.ACTION_VIEW);browserFilter.addDataScheme("https");browserFilter.addDataScheme("http");
            monitor=addMonitor(browserFilter,new ActivityResult(Activity.RESULT_CANCELED,null),true);
            home=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            final MainActivity screen=home;
            runOnMainSync(()->screen.startOrContinueSignIn());
            String url="";for(int i=0;i<60;i++){Thread.sleep(100);url=AppPreferences.getOAuthUrl(c);if(!url.isEmpty())break;}
            check(AppPreferences.isOAuthPending(c)&&!url.isEmpty(),"Actual OAuth service created a pending login");
            android.net.Uri authorize=android.net.Uri.parse(url);
            check(authorize.getHost().equals("auth.openai.com")&&authorize.getQueryParameter("client_id").equals(AppConstants.OAUTH_CLIENT_ID),"Original authorization endpoint/client preserved");
            check(authorize.getQueryParameter("code_challenge_method").equals("S256")&&!authorize.getQueryParameter("state").isEmpty(),"PKCE and state preserved");
            String redirect=authorize.getQueryParameter("redirect_uri");URI local=URI.create(redirect);check(local.getHost().equals("localhost")&&local.getPath().equals("/auth/callback")&&(local.getPort()==1455||local.getPort()==1457),"Registered loopback redirect preserved");
            Thread.sleep(1200);waitForIdleSync();
            java.lang.reflect.Field field=MainActivity.class.getDeclaredField("lastLaunchedAuthUrl");field.setAccessible(true);
            boolean received=url.equals(field.get(screen));
            check(received==phase.equals("auth-fixed"),phase.equals("auth-fixed")?"Service READY reaches main receiver and browser launch":"Reproduced 9.9.1 permission mismatch dropping READY");
            check((monitor.getHits()>0)==phase.equals("auth-fixed"),"Browser launch follows actual READY delivery");
            try(Socket socket=new Socket("127.0.0.1",local.getPort())){
                socket.setSoTimeout(5000);socket.getOutputStream().write("GET /auth/callback?state=invalid-test-state&code=never-exchange-this HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().flush();
                String status=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8)).readLine();check(status.contains("400"),"Actual callback rejects wrong state before code exchange");
            }
            check(SecureTokenStore.load(c)==null,"Rejected callback stored no credentials");
            final String before=(String)field.get(screen);c.sendBroadcast(new Intent("dev.bennett.codexmeter.action.OAUTH_READY").setPackage(c.getPackageName()).putExtra(AppConstants.EXTRA_AUTH_URL,"https://example.invalid/foreign"),AppConstants.INTERNAL_PERMISSION);Thread.sleep(300);check(java.util.Objects.equals(before,field.get(screen)),"Regular-app READY cannot drive beta login");
            result.putString("stream","Beta OAuth "+phase+": "+checks+" checks passed; actual loopback service, permission delivery, browser launch interception, PKCE/state rejection; no live code exchange.\n");code=-1;
        }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));}
        finally {
            c.startService(new Intent(c,OAuthService.class).setAction(OAuthService.ACTION_CANCEL_SILENT));
            try{Thread.sleep(400);TrialDemo.prepare(c);}catch(Exception ignored){}
            if(monitor!=null)removeMonitor(monitor);if(home!=null){MainActivity screen=home;runOnMainSync(screen::finish);}
        }
        finish(code,result);
    }
}
