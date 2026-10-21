package dev.bennett.codexmeter;
import android.app.*;import android.content.*;import android.content.res.Configuration;import android.os.*;import org.json.*;
/** Explicit local Korean context; stale workers must not bind obsolete evidence to the new plan. */
public final class Beta9913VoiceRaceNativeTest extends Instrumentation {
 int n;void check(boolean ok,String label){n++;if(!ok)throw new AssertionError(label);}
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Context original=getTargetContext();Configuration ko=new Configuration(original.getResources().getConfiguration());ko.setLocales(LocaleList.forLanguageTags("ko"));Context c=original.createConfigurationContext(ko);Bundle result=new Bundle();int code=0;UsageSnapshot saved=null;String tone=null,state=null,error=null,account=null;try{
  check(c.getPackageName().equals("dev.bennett.codexmeter.trial")&&TrialDemo.active(c),"Owned synthetic beta only");
  saved=AppPreferences.loadSnapshot(c);tone=FunStore.tone(c);error=AppPreferences.getLastError(c);account=SubscriptionStore.accountKey(c);state=c.getSharedPreferences("coach_v2",0).getString(account,"");
  c.getSharedPreferences("coach_v2",0).edit().putString(account,new JSONObject().put("schemaVersion",2).put("recent",new JSONArray()).put("tips",new JSONArray()).put("hidden",new JSONArray()).toString()).commit();FunStore.tone(c,"spicy");
  long now=System.currentTimeMillis();UsageSnapshot plus=new UsageSnapshot("plus",true,false,new UsageWindow(0,18000,0,(now+7200000)/1000),new UsageWindow(0,604800,0,(now+3*86400000L)/1000),now);
  UsageSnapshot free=new UsageSnapshot("free",true,false,null,null,new UsageWindow(0,2592000,0,(now+15*86400000L)/1000),java.util.Collections.emptyList(),null,-1,now);
  AppPreferences.saveSnapshot(c,plus);check(SpicyQuotes.select(c,account,plus,true,false,0,now)!=null,"Confirmed Plus selected in explicit Korean context");
  AppPreferences.saveSnapshot(c,free);check(SpicyQuotes.select(c,account,free,true,false,0,now)!=null,"Current Free selected");String id=SpicyQuotes.peek(c,account).id;
  check(SpicyQuotes.select(c,account,plus,true,false,0,now)==null,"OBSOLETE_PLUS_WORKER_MUST_BE_REJECTED");
  check(id.equals(SpicyQuotes.peek(c,account).id),"Current selection retained");
  result.putString("stream","Beta9913 stale worker: "+n+" checks passed; obsolete Plus snapshot cannot publish under current Free identity; synthetic only.\n");code=-1;
 }catch(Throwable t){result.putString("stream",android.util.Log.getStackTraceString(t));}finally{if(saved!=null)AppPreferences.saveSnapshot(c,saved);if(error!=null)AppPreferences.setLastError(c,error);if(tone!=null)FunStore.tone(c,tone);if(account!=null&&state!=null)c.getSharedPreferences("coach_v2",0).edit().putString(account,state).commit();}finish(code,result);}
}
