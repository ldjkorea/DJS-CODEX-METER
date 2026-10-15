package dev.bennett.codexmeter;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import org.json.JSONObject;

/** The runner stops the owned demo process between removing derived cache and opening Home. */
public final class Beta997ColdStartNativeTest extends Instrumentation {
    String phase;
    int checks;
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){super.onCreate(args);phase=args.getString("phase");start();}
    @Override public void onStart(){Bundle result=new Bundle();int code=0;try{
        Context c=getTargetContext();check(Build.MODEL.contains("sdk")&&c.getPackageName().equals("dev.bennett.codexmeter.trial")&&TrialDemo.active(c),"Owned synthetic beta only");
        String key=SubscriptionStore.accountKey(c);SharedPreferences qa=c.getSharedPreferences("beta997_cold_qa",0);
        if("seed".equals(phase)){
            JSONObject state=BetaGrowthStore.load(c,key);check(qa.edit().putString("state",state.toString()).commit(),"Checkpoint persisted");
            check(c.getSharedPreferences("beta_growth_display_v4",0).edit().remove(key).commit(),"Simulated post-DB-commit cache gap");
            check(BetaGrowthStore.peek(c)==null,"Cache absent before process stop");
        }else{
            JSONObject cached=BetaGrowthStore.peek(c);check(cached!=null,"Home repaired cache before instrumentation reads DB");
            JSONObject before=new JSONObject(qa.getString("state",""));check(cached.getLong("total")==before.getLong("total"),"Home displays original XP after cold restart");
            JSONObject after=BetaGrowthStore.load(c,key);check(before.toString().equals(after.toString()),"Cold restart preserves entire original growth document");
            check(!BetaGrowthStore.displayError(c),"Recovered home has no storage error");
        }
        result.putString("stream","Beta997 cold restart "+phase+": "+checks+" checks passed.\n");code=-1;
    }catch(Throwable error){result.putString("stream",android.util.Log.getStackTraceString(error));}finish(code,result);}
}
