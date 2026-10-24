package dev.bennett.codexmeter;

import android.content.Context;
import java.time.*;
import java.util.*;

/** Fixed-clock production formatters with resource-backed Android ports, not native layout proof. */
public final class HomeResetFormatSelfTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        Locale original=Locale.getDefault();TimeZone zone=TimeZone.getDefault();
        try{
            for(String id:new String[]{"Asia/Seoul","UTC","America/Los_Angeles","Pacific/Auckland"}){
                TimeZone.setDefault(TimeZone.getTimeZone(id));
                for(Locale locale:new Locale[]{Locale.KOREAN,Locale.ENGLISH}){
                    Locale.setDefault(locale);Context c=new Context(locale);
                    for(boolean hour24:new boolean[]{true,false}){
                        c.hour24=hour24;
                        long now=ZonedDateTime.of(2026,12,31,23,58,0,0,ZoneId.of(id)).toInstant().toEpochMilli();
                        long reset=now+4*60000;
                        UsageWindow five=new UsageWindow(25.25,18000,240,0);
                        UsageWindow week=new UsageWindow(66.75,604800,0,(reset+2*86400000)/1000);
                        String fiveText=FiveHourResetDisplay.text(c,five,now,now,WidgetOptions.RESET_BOTH,false);
                        check(fiveText.contains(UsageFormat.absolute(c,reset,now)),"Next-day absolute reset "+id);
                        check(fiveText.contains(UsageFormat.compactDuration(c,240000)),"Remaining duration retained");
                        String weekText=UsageFormat.reset(c,week,WidgetOptions.RESET_BOTH,now,now);
                        check(weekText.contains(UsageFormat.absolute(c,week.resetAtMillis(),now)),"Weekly date and time retained");
                        check(weekText.contains(UsageFormat.relative(c,week.resetAtMillis(),now)),"Weekly countdown retained");
                        String schedule=c.getString(R.string.home_quota_reset_schedule,UsageFormat.absolute(c,reset,now),UsageFormat.compactDuration(c,reset-now));
                        check(schedule.split("\n").length==2&&schedule.startsWith(UsageFormat.absolute(c,reset,now)),"Schedule separates absolute time and countdown");
                        check(FiveHourResetState.at(five,now,reset-1).kind==FiveHourResetState.Kind.COUNTDOWN,"Before reset");
                        check(FiveHourResetState.at(five,now,reset).kind==FiveHourResetState.Kind.EXPIRED,"Exact reset not full quota");
                        check(FiveHourResetDisplay.text(c,five,now,reset,WidgetOptions.RESET_BOTH,false).contains(c.getString(R.string.pub_five_expired)),"Expired notice");
                        check(FiveHourResetDisplay.text(c,new UsageWindow(10,18000,0,0),now,now,WidgetOptions.RESET_BOTH,false).contains(c.getString(R.string.pub_five_hour_reset_unknown)),"Unknown reset not invented");
                        AppPreferences.error="offline";
                        check(FiveHourResetDisplay.text(c,five,now,now,WidgetOptions.RESET_BOTH,false).contains(c.getString(R.string.pub_five_error)),"Failure marked cached");
                        AppPreferences.error="";
                        check(FiveHourResetDisplay.text(c,five,now-3600000,now-3600000+1000000,WidgetOptions.RESET_BOTH,false).contains(c.getString(R.string.pub_five_hour_reset_stale)),"Stale remains marked");
                    }
                }
            }
            // Both DST directions use server epochs and local civil time, never 24-hour day arithmetic.
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));Locale.setDefault(Locale.KOREAN);Context c=new Context(Locale.KOREAN);c.hour24=true;
            for(String start:new String[]{"2026-03-08T09:30:00Z","2026-11-01T08:30:00Z"}){
                long now=Instant.parse(start).toEpochMilli();UsageWindow w=new UsageWindow(10,18000,7200,0);
                String text=FiveHourResetDisplay.text(c,w,now,now,WidgetOptions.RESET_BOTH,false);
                check(text.contains(UsageFormat.absolute(c,now+7200000,now)),"DST local time");
                check(FiveHourResetState.at(w,now,now).remainingMillis==7200000,"DST epoch duration");
            }
            System.out.println("Home reset format: "+checks+" checks passed; real formatters/resources, fixed clocks and mock Android ports.");
        }finally{AppPreferences.error="";Locale.setDefault(original);TimeZone.setDefault(zone);}
    }
}
