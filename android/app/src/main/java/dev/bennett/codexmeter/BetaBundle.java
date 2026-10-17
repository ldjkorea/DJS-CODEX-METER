package dev.bennett.codexmeter;

import android.content.Context;
import org.json.*;

/** Selected sections only. Credentials are never imported through this format. */
final class BetaBundle {
    static JSONObject collect(Context c,String account,boolean growth,boolean settings)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){
            if(!BetaGrowthStore.current(c,account)||!growth&&!settings)throw new IllegalArgumentException("Account/selection");
            JSONObject out=new JSONObject().put("format","gpt-hud-beta-bundle").put("version",1).put("account",account).put("created_at",System.currentTimeMillis());
            if(growth){out.put("growth",BetaGrowthStore.export(c,account));JSONObject medals=BetaMedalStore.export(c,account);if(medals!=null)out.put("medals",medals);}
            if(settings)out.put("personal",BetaPreferences.export(c,account)).put("settings",SettingsTransferStore.collect(c,true,true,true,false).toJson());
            return out;
        }
    }
    static boolean bundle(JSONObject file){return "gpt-hud-beta-bundle".equals(file.optString("format"));}
    static JSONObject preview(Context c,String account,JSONObject file,boolean growth,boolean settings)throws Exception {
        if(!bundle(file)){if(!growth)throw new IllegalArgumentException("Legacy file has growth only");return BetaGrowthStore.preview(c,account,file);}
        if(file.getInt("version")!=1||!account.equals(file.getString("account"))||!BetaGrowthStore.current(c,account))throw new IllegalArgumentException("Backup account/version");
        java.util.Iterator<String> keys=file.keys();while(keys.hasNext())if(!java.util.Arrays.asList("format","version","account","created_at","growth","medals","personal","settings").contains(keys.next()))throw new IllegalArgumentException("Unknown section");
        JSONObject result=BetaGrowthStore.load(c,account);
        if(growth){if(!file.has("growth"))throw new IllegalArgumentException("No growth section");result=BetaGrowthStore.preview(c,account,file.getJSONObject("growth"));}
        if(growth&&file.has("medals"))BetaMedalStore.validate(c,account,file.getJSONObject("medals"));
        if(settings){BetaPreferences.validate(file.getJSONObject("personal"));SettingsTransfer.Document document=SettingsTransfer.parse(file.getJSONObject("settings"));if(document.hasAuthentication()||!document.hasAppSettings()||!document.hasNotifications()||!document.hasNowBar())throw new IllegalArgumentException("Settings sections/authentication");}
        if(!growth&&!settings)throw new IllegalArgumentException("No sections selected");return result==null?new JSONObject().put("total",0).put("honors",new JSONObject()).put("titles",new JSONObject()):result;
    }
    static String restore(Context c,String account,JSONObject file,boolean growth,boolean settings)throws Exception {
        synchronized(UsageApi.NETWORK_LOCK){
            preview(c,account,file,growth,settings);
            if(!bundle(file)){if(!BetaGrowthStore.restore(c,account,file))throw new java.io.IOException("Account changed");return BetaExperienceUi.t(c,"성장 기록 병합 완료","Growth merged");}
            StringBuilder result=new StringBuilder();
            if(growth){try{if(!BetaGrowthStore.restore(c,account,file.getJSONObject("growth")))throw new java.io.IOException();if(file.has("medals"))BetaMedalStore.restore(c,account,file.getJSONObject("medals"));result.append(BetaExperienceUi.t(c,"성장 기록 병합 완료","Growth merged"));}catch(Exception error){throw new java.io.IOException(BetaExperienceUi.t(c,"성장 반영 중 오류. 일부 저장됐을 수 있으니 상태를 확인하세요. 설정은 적용하지 않았습니다.","Growth merge failed; some data may have been saved. Check retained records. Settings were not applied."),error);}}
            if(settings){
                if(result.length()>0)result.append("\n");
                try{if(!BetaGrowthStore.current(c,account))throw new IllegalStateException();SettingsTransferStore.apply(c,SettingsTransfer.parse(file.getJSONObject("settings")),true,true,true,false);BetaPreferences.restore(c,account,file.getJSONObject("personal"));result.append(BetaExperienceUi.t(c,"설정 적용 완료 · 로그인 정보 제외","Settings applied · authentication excluded"));}
                catch(Exception error){result.append(BetaExperienceUi.t(c,"설정 적용 중 오류 · 일부 설정이 적용됐을 수 있습니다. 성장 결과는 위와 같습니다.","Settings failed; some settings may have been applied. Growth result is shown above."));}
            }
            return result.toString();
        }
    }
}
