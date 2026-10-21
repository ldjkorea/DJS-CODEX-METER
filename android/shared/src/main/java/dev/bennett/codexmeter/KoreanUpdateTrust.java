package dev.bennett.codexmeter;
import java.net.URI;
/** Public release metadata is only a source hint; the pinned signing key is the trust anchor. */
public final class KoreanUpdateTrust {
    public static final String CERTIFICATE="6abd39b13e561cd67fa0d76ab79f9f22500cf5378dc60b176bfe18bf0d2f8be3";
    public static final String REPOSITORY="https://github.com/ldjkorea/DJS-GPT-HUD";
    public static String releaseRoot(String url){try{URI u=URI.create(url);
        if(!"https".equals(u.getScheme())||!"github.com".equals(u.getHost())||u.getUserInfo()!=null||u.getPort()!=-1
            ||u.getQuery()!=null||u.getFragment()!=null||u.getPath().contains("..")||u.getRawPath().contains("%"))return "";
        for(String path:new String[]{"/ldjkorea/DJS-GPT-HUD","/ldjkorea/DJS-CODEX-METER","/ldjkorea/codex-meter-ko"})
            if(u.getPath().startsWith(path+"/releases/"))return "https://github.com"+path;
        return "";
    }catch(Exception e){return "";}}
    public static boolean official(String url){return !releaseRoot(url).isEmpty();}
    public static boolean compatible(String pkg,String cert,long installed,long downloaded){return "dev.bennett.codexmeter".equals(pkg)&&CERTIFICATE.equals(cert)&&downloaded>installed;}
}
