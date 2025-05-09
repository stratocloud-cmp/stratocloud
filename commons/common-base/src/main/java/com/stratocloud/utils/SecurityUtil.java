package com.stratocloud.utils;

import com.stratocloud.exceptions.StratoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.util.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;


@Slf4j
public class SecurityUtil {
    private static final String MD5_SALT = "&%5673***&&%%$$#@";

    public static String toSaltMD5(String str) {
        String base = str +"/"+MD5_SALT;
        return DigestUtils.md5DigestAsHex(base.getBytes());
    }

    public static String toMD5(String str) {
        return DigestUtils.md5DigestAsHex(str.getBytes());
    }


    public static String AESEncrypt(String sSrc, String sKey) {
        Assert.nonBlank(sSrc, sKey);
        validateKeyLength(sKey);
        try {
            byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(sSrc.getBytes(StandardCharsets.UTF_8));

            return Hex.encodeHexString(encrypted);
        }catch (Exception e){
            throw new StratoException(e.getMessage(), e);
        }
    }

    private static void validateKeyLength(String sKey) {
        if (Utils.isBlank(sKey) || sKey.length() != 16) {
            log.warn("Key length is not 16");
            throw new IllegalArgumentException();
        }
    }


    public static String AESDecrypt(String sSrc, String sKey) {
        try {
            Assert.nonBlank(sSrc, sKey);
            validateKeyLength(sKey);
            byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] encrypted1 = hexStringToByteArray(sSrc);
            try {
                byte[] original = cipher.doFinal(encrypted1);
                return new String(original, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("AES decrypt failure, source={}, key={}",sSrc, sKey,e);
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            log.error("AES decrypt failure, source={}, key={}",sSrc, sKey,ex);
            throw new IllegalArgumentException();
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }






    public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        return createIgnoreVerifySSL("TLSv1.2");
    }

    public static SSLContext createIgnoreVerifySSL(String protocol) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance(protocol);
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(
                    X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) {
            }

            @Override
            public void checkServerTrusted(
                    X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        sc.init(null, new TrustManager[] { trustManager }, new SecureRandom());
        return sc;
    }

    public static String encodeToBase64(String s){
        byte[] bytes = Base64.getEncoder().encode(s.getBytes());
        return new String(bytes);
    }

    public static String decodeFromBase64(String s){
        if(Utils.isBlank(s))
            return s;
        byte[] bytes = Base64.getDecoder().decode(s);
        return new String(bytes);
    }

    public static boolean isSensitiveProperty(String propertyName){
        if(Utils.isBlank(propertyName))
            return false;
        String lowerCase = propertyName.toLowerCase();
        return lowerCase.contains("pass") ||
                lowerCase.contains("key") ||
                lowerCase.contains("secret");
    }
}
