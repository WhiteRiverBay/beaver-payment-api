package ltd.wrb.payment.util;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import org.bouncycastle.util.encoders.Base64;

import cn.hutool.core.io.FileUtil;

public class EncryptUtil {

    /**
     * 1, generate random aes key
     * 2, encrypt data with aes key
     * 3, encrypt aes key with rsa public key
     * 4, return encrypted data and encrypted aes key as hex string array
     * 
     * @param data
     * @param publicKey
     * @return encrypted data and encrypted aes key as hex string array
     * @throws Exception
     */
    // public static String[] encryptToHex(String data, PublicKey publicKey) throws Exception {
    //     SecretKey key = AESUtil.generateAESKey();

    //     String encryptedData = AESUtil.encryptToHexString(key, data);
    //     byte[] encryptedKey = RSAUtil.encryptByPublicKey(key.getEncoded(), publicKey);

    //     return new String[] { encryptedData, Hex.toHexString(encryptedKey) };
    // }

    public static String[] encrypt(String data, PublicKey publicKey) throws Exception {
        SecretKey aesKey = AESUtil.generateAESKey();

        byte[] encryptedKey = RSAUtil.encryptByPublicKey(aesKey.getEncoded(), publicKey);

        String encryptedData = AESUtil.encryptToBase64(aesKey, data);
        String base64EncryptedKey = Base64.toBase64String(encryptedKey);

        return new String[] { encryptedData, base64EncryptedKey };
    }

    /**
     * 1, decrypt aes key with rsa private key
     * 2, recover aes key from hex string
     * 3, decrypt data with aes key
     * 
     * @param encryptData
     * @param encryptAesKey
     * @param privateKey
     * @return decrypted data
     * @throws Exception
     */
    public static String decrypt(String encryptData, String encryptAesKey, PrivateKey privateKey) throws Exception {
        byte[] aesKey = RSAUtil.decryptByPrivateKey(Base64.decode(encryptAesKey), privateKey);
        // System.out.println("aesKey: " + Base64.toBase64String(aesKey));
        SecretKey key = AESUtil.recoverAESKeyFromBase64(Base64.toBase64String(aesKey));
        return AESUtil.decryptFromBase64(key, encryptData);
    }

    // test decrypt

    public static void main(String[] args) throws Exception {
        String data = "";
        String aesKey = "";

        String privateKey = "private.pem";
        String privateKeyPem = FileUtil.readUtf8String(privateKey);
        String decryptedData = decrypt(data, aesKey, RSAUtil.getPrivateKeyFromPem(privateKeyPem));
        System.out.println("Decrypted Data: " + decryptedData);
    }

}
