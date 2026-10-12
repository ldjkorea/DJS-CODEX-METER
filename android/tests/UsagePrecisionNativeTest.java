package dev.bennett.codexmeter;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import java.io.*;

/** Production settings UI and formatter on the owned synthetic emulator only. */
public final class UsagePrecisionNativeTest extends Instrumentation {
    int checks;
    void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();Context c=getTargetContext();
        android.content.SharedPreferences prefs=c.getSharedPreferences("codex_meter_settings_v1",0);
        boolean had=prefs.contains("usage_decimal_places");int previous=prefs.getInt("usage_decimal_places",3);
        try{
            check(Build.MODEL.contains("sdk"),"Emulator only");
            check(LanSync.account(c).equals(hash("refinement-production-fixture-account")),"Owned synthetic account only");
            check(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals("2.8.29"),"Final release installed");
            String original=AppPreferences.loadSnapshot(c).toJson().toString();
            for(int digits:new int[]{2,3}){
                check(AppPreferences.setUsageDecimalPlaces(c,digits),"Preference commit");
                check(AppPreferences.usageDecimalPlaces(c.createPackageContext(c.getPackageName(),0))==digits,"Persisted choice read by new context");
                Activity activity=startActivitySync(new Intent(c,MeterSettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
                waitForIdleSync();Thread.sleep(800);
                final Throwable[] failed={null};runOnMainSync(()->{try{
                    View decor=activity.getWindow().getDecorView();TextView button=find(decor,activity.getString(R.string.usage_decimal_current,digits));
                    check(button!=null,"Actual settings button shows "+digits);
                    check(button.getWidth()>0&&button.getHeight()>0,"Button laid out");
                    check(button.performClick(),"Button opens chooser");
                    java.lang.reflect.Method method=WidgetRenderer.class.getDeclaredMethod("preciseSlotText",Context.class,UsageWindow.class,boolean.class,boolean.class);method.setAccessible(true);
                    UsageWindow window=new UsageWindow(13.42736,604800,0,0);
                    String expected=digits==2?"86.57%":"86.573%";
                    check(expected.equals(method.invoke(null,c,window,false,true)),"Widget selected precision");
                    check(expected.equals(UsagePrecision.remaining(window,digits)),"Home uses same precision");
                    check("—".equals(method.invoke(null,c,null,false,true)),"Unknown widget data remains unknown");
                    check("13.42736".equals(Double.toString(window.preciseUsedPercent)),"Raw precision unchanged");
                }catch(Throwable e){failed[0]=e;}});if(failed[0]!=null)throw failed[0];waitForIdleSync();
                Bitmap screenshot=getUiAutomation().takeScreenshot();
                try(FileOutputStream file=new FileOutputStream(new File(c.getExternalFilesDir(null),"precision-"+digits+".png"))){screenshot.compress(Bitmap.CompressFormat.PNG,100,file);}finally{screenshot.recycle();}
                android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
                String choice=c.getString(digits==2?R.string.usage_decimal_three:R.string.usage_decimal_two);
                java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText(choice);
                check(!nodes.isEmpty(),"Choice visible in actual dialog");
                android.graphics.Rect bounds=new android.graphics.Rect();nodes.get(0).getBoundsInScreen(bounds);
                long touch=SystemClock.uptimeMillis();
                MotionEvent down=MotionEvent.obtain(touch,touch,MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0);
                MotionEvent up=MotionEvent.obtain(touch,touch+80,MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0);
                down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
                try{check(getUiAutomation().injectInputEvent(down,false)&&getUiAutomation().injectInputEvent(up,false),"Tap actual precision choice");}finally{down.recycle();up.recycle();}
                for(int retry=0;retry<30&&AppPreferences.usageDecimalPlaces(c)==digits;retry++)Thread.sleep(100);
                check(AppPreferences.usageDecimalPlaces(c)==(digits==2?3:2),"UI selection persisted");
                Thread.sleep(800);
            }
            AppPreferences.setUsageDecimalPlaces(c,3);
            check(!AppPreferences.setUsageDecimalPlaces(c,9)&&AppPreferences.usageDecimalPlaces(c)==3,"Invalid choice rejected");
            check(original.equals(AppPreferences.loadSnapshot(c).toJson().toString()),"Settings leave stored observation unchanged");
            result.putString("stream","Usage precision native: "+checks+" checks passed; actual settings screen, persistence and widget/home formatting.\n");finish(-1,result);
        }catch(Throwable e){result.putString("stream",android.util.Log.getStackTraceString(e));finish(0,result);}
        finally{if(had)prefs.edit().putInt("usage_decimal_places",previous).commit();else prefs.edit().remove("usage_decimal_places").commit();}
    }
    TextView find(View v,String text){if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return (TextView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView found=find(g.getChildAt(i),text);if(found!=null)return found;}}return null;}
    String hash(String value)throws Exception{byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString();}
}
