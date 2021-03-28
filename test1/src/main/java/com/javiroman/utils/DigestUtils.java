package com.javiroman.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class DigestUtils {

    private final static Logger LOGGER = LoggerFactory.getLogger(DigestUtils.class);

    private static final String CHARACTERS = "ABCDEF1234567890";

    private final static String NC = "00000001";

    public static String digest(String username, String password, String realm, String nonce, String qop, String cnonce,
                                String uri, String method) {
        //HA1 = MD5("usarname:realm:password");
        //HA2 = MD5("httpmethod:uri");
        //response = MD5("HA1:nonce:nc:cnonce:qop:HA2");
        if (cnonce == null) {
            cnonce = DigestUtils.randomString(32);
        }
        String ha1 = md5sums(username + ":" + realm + ":" + password);
        String ha2 = md5sums(method + ":" + uri);
        String reponse = md5sums(ha1 + ":" + nonce + ":" + NC + ":" + cnonce + ":" + qop + ":" + ha2);
        StringBuffer strBuffer = new StringBuffer();
        strBuffer.append("Digest username=\"");
        strBuffer.append(username);
        strBuffer.append("\",realm=\"");
        strBuffer.append(realm);
        strBuffer.append("\",qop=\"");
        strBuffer.append(qop);
        strBuffer.append("\",algorithm=\"MD5\"");
        strBuffer.append(",uri=\"");
        strBuffer.append(uri);
        strBuffer.append("\",nonce=\"");
        strBuffer.append(nonce);
        strBuffer.append("\",nc=" + NC + ",cnonce=\"");
        strBuffer.append(cnonce);
        strBuffer.append("\",response=\"");
        strBuffer.append(reponse);
        strBuffer.append("\"");
        return strBuffer.toString();
    }

    public static String md5sums(String input) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("MD5计算异常", e);
        }
        byte[] inputByteArray = input.getBytes();
        messageDigest.update(inputByteArray);
        byte[] resultByteArray = messageDigest.digest();
        return byteArrayToHex(resultByteArray);
    }

    public static String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }

    public static String randomString(int length) {
        Random random = new Random(System.currentTimeMillis());
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = CHARACTERS.charAt(random.nextInt(CHARACTERS.length()));
        }
        return new String(text);
    }

    public static String encodeBase64(String message){
        return Base64.encodeBase64String(message.getBytes());
    }

    public static String getFileMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return new String(Hex.encodeHex(MD5.digest()));
        } catch (Exception e) {
            LOGGER.error("计算文件MD5异常", e);
            return null;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("", e);
            }
        }
    }

}
