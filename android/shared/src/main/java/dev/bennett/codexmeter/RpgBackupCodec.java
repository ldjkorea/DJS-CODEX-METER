package dev.bennett.codexmeter;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.json.*;

/** Portable authenticated encryption. No device key, auth token or password is persisted. */
public final class RpgBackupCodec {
    public static final int MAX_BYTES=32*1024*1024, ITERATIONS=600000;
    private static final String FORMAT="gpt-hud-rpg-encrypted", AAD=FORMAT+"|1|PBKDF2-HMAC-SHA256|600000|AES-256-GCM";
    private RpgBackupCodec(){}
    private static SecretKeySpec key(char[] password,byte[] salt)throws Exception {
        if(password==null||password.length<10||password.length>256)throw new IllegalArgumentException("Password length");
        PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,256);byte[] raw=null;
        try{raw=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();return new SecretKeySpec(raw,"AES");}
        finally{spec.clearPassword();if(raw!=null)Arrays.fill(raw,(byte)0);}
    }
    public static byte[] encrypt(JSONObject payload,char[] password)throws Exception {
        byte[] plain=payload.toString().getBytes(StandardCharsets.UTF_8);if(plain.length>MAX_BYTES)throw new IllegalArgumentException("Backup too large");
        byte[] salt=new byte[16],nonce=new byte[12];SecureRandom random=new SecureRandom();random.nextBytes(salt);random.nextBytes(nonce);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),new GCMParameterSpec(128,nonce));cipher.updateAAD(AAD.getBytes(StandardCharsets.UTF_8));
        try{return new JSONObject().put("format",FORMAT).put("version",1).put("iterations",ITERATIONS)
            .put("salt",Base64.getEncoder().encodeToString(salt)).put("nonce",Base64.getEncoder().encodeToString(nonce))
            .put("data",Base64.getEncoder().encodeToString(cipher.doFinal(plain))).toString().getBytes(StandardCharsets.UTF_8);}
        finally{Arrays.fill(plain,(byte)0);}
    }
    public static JSONObject decrypt(byte[] bytes,char[] password)throws Exception {
        if(bytes==null||bytes.length>MAX_BYTES*3/2)throw new IllegalArgumentException("Backup size");
        JSONObject file=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        // JSONObject.getInt truncates fractional values; reject altered protocol parameters.
        if(!FORMAT.equals(file.getString("format"))||!Integer.valueOf(1).equals(file.get("version"))||!Integer.valueOf(ITERATIONS).equals(file.get("iterations"))||file.length()!=6)throw new IllegalArgumentException("Backup format");
        byte[] salt=Base64.getDecoder().decode(file.getString("salt")),nonce=Base64.getDecoder().decode(file.getString("nonce")),data=Base64.getDecoder().decode(file.getString("data"));
        if(salt.length!=16||nonce.length!=12||data.length<16||data.length>MAX_BYTES+16)throw new IllegalArgumentException("Backup parameters");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(password,salt),new GCMParameterSpec(128,nonce));cipher.updateAAD(AAD.getBytes(StandardCharsets.UTF_8));
        byte[] plain=cipher.doFinal(data);try{return new JSONObject(new String(plain,StandardCharsets.UTF_8));}finally{Arrays.fill(plain,(byte)0);}
    }
}
