package com.dsh.privatememo;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyStore;
import java.security.SecureRandom;

/** AES-256-GCM 加解密；主密钥用 Android Keystore 保护 */
public class Crypto {
    private static final String KS = "AndroidKeyStore";
    private static final String ALIAS = "pm_kek_v1";

    private static SecretKey kek() throws Exception {
        KeyStore ks = KeyStore.getInstance(KS);
        ks.load(null);
        if (ks.containsAlias(ALIAS)) return (SecretKey) ks.getKey(ALIAS, null);
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS);
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return kg.generateKey();
    }

    /** 用 Keystore 主密钥包装任意字节（返回 base64 iv:ct） */
    public static String wrap(byte[] plain) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, kek());
        byte[] iv = c.getIV(), ct = c.doFinal(plain);
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ct, Base64.NO_WRAP);
    }

    public static byte[] unwrap(String s) throws Exception {
        String[] p = s.split(":");
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, kek(), new GCMParameterSpec(128, Base64.decode(p[0], Base64.NO_WRAP)));
        return c.doFinal(Base64.decode(p[1], Base64.NO_WRAP));
    }

    /** 用 vault 密钥加密数据（返回 base64 iv:ct） */
    public static String seal(byte[] key, byte[] data) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] iv = c.getIV(), ct = c.doFinal(data);
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ct, Base64.NO_WRAP);
    }

    public static byte[] open(byte[] key, String s) throws Exception {
        String[] p = s.split(":");
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, Base64.decode(p[0], Base64.NO_WRAP)));
        return c.doFinal(Base64.decode(p[1], Base64.NO_WRAP));
    }

    public static byte[] random(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }

    /** 密码哈希（PBKDF2-SHA256，10000 轮） */
    public static String hashPwd(String pwd, String saltB64) throws Exception {
        byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] h = f.generateSecret(new PBEKeySpec(pwd.toCharArray(), salt, 10000, 256)).getEncoded();
        return Base64.encodeToString(h, Base64.NO_WRAP);
    }
}
