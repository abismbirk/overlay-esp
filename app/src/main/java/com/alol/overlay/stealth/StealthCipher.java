package com.alol.overlay.stealth;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class StealthCipher {
    private static final String ALGO = "AES/GCM/NoPadding";
    private static byte[] key = "1234567890123456".getBytes();
    public static String encrypt(String data) throws Exception {
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, key));
        byte[] enc = c.doFinal(data.getBytes());
        return Base64.encodeToString(enc, Base64.DEFAULT);
    }
    public static String decrypt(String data) throws Exception {
        Cipher c = Cipher.getInstance(ALGO);
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, key));
        byte[] dec = c.doFinal(Base64.decode(data, Base64.DEFAULT));
        return new String(dec);
    }
}
