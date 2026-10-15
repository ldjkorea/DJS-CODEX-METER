package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Real loopback service + encrypted synthetic credentials; never exchanges a real OAuth code. */
public final class Beta997AuthRecoveryNativeTest extends Instrumentation {
    int checks;Throwable uiFailure;
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    void main(Runnable action){runOnMainSync(()->{try{action.run();}catch(Throwable t){uiFailure=t;}});if(uiFailure!=null){Throwable problem=uiFailure;uiFailure=null;throw new AssertionError(problem);}}
    String waitUrl(Context c,String previous)throws Exception{for(int i=0;i<100;i++){String url=AppPreferences.getOAuthUrl(c);if(!url.isEmpty()&&!url.equals(previous))return url;Thread.sleep(100);}throw new AssertionError("New authorization URL was not created");}
    String callback(Uri authorize,String state,String query)throws Exception{Uri local=Uri.parse(authorize.getQueryParameter("redirect_uri"));try(Socket s=new Socket("127.0.0.1",local.getPort())){s.setSoTimeout(5000);s.getOutputStream().write(("GET /auth/callback?state="+state+"&"+query+" HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));s.getOutputStream().flush();return new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8)).readLine();}}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;Context c=getTargetContext();AuthTokens original=null;UsageSnapshot snapshot=null;MainActivity home=null;ActivityMonitor monitor=null;boolean automatic=false,launch=false;
        try{
            check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter.trial")&&TrialDemo.active(c),"Owned synthetic beta only");
            original=SecureTokenStore.load(c);snapshot=AppPreferences.loadSnapshot(c);automatic=AppPreferences.getAutomaticRefresh(c);launch=AppPreferences.getRefreshOnLaunch(c);
            AuthTokens a=new AuthTokens("SYNTHETIC_LATE_A","SYNTHETIC_REFRESH_A","",System.currentTimeMillis()+3600000,"beta997-synthetic-a","");
            AuthTokens b=new AuthTokens("SYNTHETIC_CURRENT_B","SYNTHETIC_REFRESH_B","",System.currentTimeMillis()+3600000,"beta997-synthetic-b","");
            long stale=AccountSession.beginSignIn();AccountSession.cancelSignIn();check(!AccountSession.installFromOAuth(c,a,stale),"Cancelled token exchange cannot commit");check(TrialDemo.active(c),"Cancel retains original account");
            long old=AccountSession.beginSignIn();AccountSession.install(c,b,false);check(!AccountSession.installFromOAuth(c,a,old),"Late callback cannot overwrite another installed account");check(SecureTokenStore.load(c).accountId.equals(b.accountId),"New account remains installed");
            long current=AccountSession.beginSignIn();check(AccountSession.installFromOAuth(c,a,current),"Current synthetic callback commits");check(!AccountSession.installFromOAuth(c,b,current),"Duplicate callback cannot commit again");
            AccountSession.install(c,original,false);if(snapshot!=null)AppPreferences.saveSnapshot(c,snapshot);
            TrialDemo.leaveDemo(c);AppPreferences.setAutomaticRefresh(c,false);AppPreferences.setRefreshOnLaunch(c,false);
            IntentFilter browsers=new IntentFilter(Intent.ACTION_VIEW);browsers.addDataScheme("https");browsers.addDataScheme("http");monitor=addMonitor(browsers,new ActivityResult(Activity.RESULT_CANCELED,null),true);
            home=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));MainActivity screen=home;
            main(screen::startOrContinueSignIn);Uri first=Uri.parse(waitUrl(c,""));check(first.getHost().equals("auth.openai.com")&&"S256".equals(first.getQueryParameter("code_challenge_method")),"Original authorization endpoint and PKCE retained");
            main(screen::cancelSignIn);check(!AccountSession.signInActive()&&!AppPreferences.isOAuthPending(c),"Cancel invalidates attempt immediately");Thread.sleep(250);
            main(screen::startOrContinueSignIn);Uri retry=Uri.parse(waitUrl(c,first.toString()));check(!first.getQueryParameter("state").equals(retry.getQueryParameter("state")),"Retry has fresh PKCE state");
            check(callback(retry,first.getQueryParameter("state"),"code=never-exchange").contains("400"),"Old state rejected by current loopback listener");check(SecureTokenStore.load(c)==null,"Old state installs no token");
            java.lang.reflect.Field browser=MainActivity.class.getDeclaredField("lastLaunchedAuthUrl");browser.setAccessible(true);Thread.sleep(300);Object before=browser.get(screen);
            c.sendBroadcast(new Intent(AppConstants.ACTION_OAUTH_READY).setPackage(c.getPackageName()).putExtra(AppConstants.EXTRA_AUTH_URL,"https://example.invalid/old-login").putExtra("beta_oauth_attempt",stale),AppConstants.INTERNAL_PERMISSION);Thread.sleep(300);check(java.util.Objects.equals(before,browser.get(screen)),"Queued obsolete READY cannot reopen old browser");
            check(callback(retry,retry.getQueryParameter("state"),"error=access_denied").contains("400"),"User-declined callback handled locally");
            for(int i=0;i<60&&AppPreferences.isOAuthPending(c);i++)Thread.sleep(100);check(!AppPreferences.isOAuthPending(c),"Failure clears pending state without token loss");
            Thread.sleep(200);main(screen::startOrContinueSignIn);Uri third=Uri.parse(waitUrl(c,retry.toString()));check(!third.getQueryParameter("state").equals(retry.getQueryParameter("state")),"Failure can be retried again");main(screen::cancelSignIn);
            check(monitor.getHits()>0,"Actual READY reached browser launch interception");check(SecureTokenStore.load(c)==null,"No real code exchange or credentials created");
            result.putString("stream","Beta997 auth recovery: "+checks+" checks passed; actual service/cancel/retry/loopback/late commit fence; synthetic only.\n");code=-1;
        }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}
        finally{try{AccountSession.cancelSignIn();c.startService(new Intent(c,OAuthService.class).setAction(OAuthService.ACTION_CANCEL_SILENT));Thread.sleep(300);if(home!=null){MainActivity screen=home;main(screen::finish);}if(monitor!=null)removeMonitor(monitor);if(original!=null){AccountSession.install(c,original,false);if(snapshot!=null)AppPreferences.saveSnapshot(c,snapshot);AppPreferences.setAutomaticRefresh(c,automatic);AppPreferences.setRefreshOnLaunch(c,launch);}}catch(Throwable cleanup){code=0;result.putString("stream",result.getString("stream","")+cleanup);}}
        finish(code,result);
    }
}
