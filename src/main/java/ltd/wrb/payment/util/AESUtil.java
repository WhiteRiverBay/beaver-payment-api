package ltd.wrb.payment.util;

import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

public class AESUtil {
    private static final int KEY_SIZE = 256; // AES 256 bits

    private static final int GCM_TAG_LENGTH = 128; // GCM 标签长度
    private static final int GCM_IV_LENGTH = 12; // GCM IV 长度

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");
        keyGen.init(KEY_SIZE);
        return keyGen.generateKey();
    }

    public static SecretKey recoverAESKeyFromHex(String hexKey) {
        byte[] keyBytes = Hex.decode(hexKey);
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static SecretKey recoverAESKeyFromBase64(String hexKey) {
        byte[] keyBytes = Base64.decode(hexKey);
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static byte[] encrypt(SecretKey aesKey, byte[] inputData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] encryptedData = cipher.doFinal(inputData);

        byte[] result = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);

        return result;
    }

    public static String encryptToHexString(SecretKey aesKey, String data) throws Exception {
        byte[] encryptedData = encrypt(aesKey, data.getBytes());
        return Hex.toHexString(encryptedData);
    }

    public static String encryptToBase64(SecretKey aesKey, String data) throws Exception {
        byte[] encryptedData = encrypt(aesKey, data.getBytes());
        return Base64.toBase64String(encryptedData);
    }

    public static String decryptFromHexString(SecretKey aesKey, String hexEncryptedData) throws Exception {
        byte[] encryptedData = Hex.decode(hexEncryptedData);
        byte[] decryptedData = decrypt(aesKey, encryptedData);
        return new String(decryptedData);
    }

    // decryptFromBase64
    public static String decryptFromBase64(SecretKey aesKey, String base64EncryptedData) throws Exception {
        byte[] encryptedData = Base64.decode(base64EncryptedData);
        byte[] decryptedData = decrypt(aesKey, encryptedData);
        return new String(decryptedData);
    }

    public static byte[] decrypt(SecretKey aesKey, byte[] encryptedData) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
        
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(Cipher.DECRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(GCM_TAG_LENGTH, iv));

        return cipher.doFinal(ciphertext);
    }

    public static void main(String[] args) throws Exception {
        SecretKey aesKey = recoverAESKeyFromHex("b8951ec311954bd2dd1c60fd8130b282b78e5210365bf2c27fae94f4283c334e"); // generateAESKey();

        System.out.println("KEY = " + Hex.toHexString(aesKey.getEncoded()));

        String originalText = "Hello, AES Encryption with Bouncy Castle!";
        byte[] encryptedData = encrypt(aesKey, originalText.getBytes());
        byte[] decryptedData = decrypt(aesKey, encryptedData);

        System.out.println("Original: " + originalText);
        System.out.println("Encrypted: " + Hex.toHexString(encryptedData));
        //base 64
        System.out.println("Encrypted: " + Base64.toBase64String(encryptedData));
        System.out.println("Decrypted: " + new String(decryptedData));

    }
}
