package ltd.wrb.payment.util;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.Hex;

public class RSAUtil {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // 生成密钥对
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048); // 2048位的密钥对，越大越安全，但是性能越差
        return keyPairGenerator.generateKeyPair();
    }

    // 获取公钥
    // public static PublicKey getPublicKey(String publicKey) throws
    // NoSuchAlgorithmException, InvalidKeySpecException {
    // KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    // X509EncodedKeySpec x509EncodedKeySpec = new
    // X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
    // return keyFactory.generatePublic(x509EncodedKeySpec);
    // }

    // 从pem里获取4096位的公钥
    public static PublicKey getPublicKeyFromPem(String pem)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        try (PEMParser pemParser = new PEMParser(new java.io.StringReader(pem))) {
            SubjectPublicKeyInfo subjectPublicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
            // X509EncodedKeySpec x509EncodedKeySpec = new
            // X509EncodedKeySpec(subjectPublicKeyInfo.getEncoded());
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            PublicKey publicKey = converter.getPublicKey(subjectPublicKeyInfo);
            return publicKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get public key from pem", e);
        }
    }

    // 获取私钥
    public static PrivateKey getPrivateKey(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
        return keyFactory.generatePrivate(pkcs8EncodedKeySpec);
    }

    // 从pem里获取4096位的私钥
    public static PrivateKey getPrivateKeyFromPem(String pem)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        try (PEMParser pemParser = new PEMParser(new java.io.StringReader(pem))) {
            Object object = pemParser
                    .readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            if (object instanceof PEMEncryptedKeyPair) {
                throw new PEMException("Encrypted private keys are not supported in this example.");
            } else if (object instanceof PEMKeyPair) {
                // PEMKeyPair 对象表示未加密的私钥
                PEMKeyPair keyPair = (PEMKeyPair) object;
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (object instanceof PrivateKeyInfo) {
                // PrivateKeyInfo 对象表示未加密的私钥
                PrivateKeyInfo keyInfo = (PrivateKeyInfo) object;
                PrivateKey privateKey = converter.getPrivateKey(keyInfo);
                // System.out.println(privateKey.getFormat()); // pkcs8格式
                return privateKey;
            } else {
                System.out.println(object.getClass());
                throw new IllegalArgumentException("Invalid PEM file: Not a private key");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get private key from pem", e);
        }
    }

    // public static byte[] encryptByPrivateKey(byte[] content, PrivateKey privateKey) throws NoSuchAlgorithmException,
    //         NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    //     Cipher cipher = Cipher.getInstance("RSA");
    //     cipher.init(Cipher.ENCRYPT_MODE, privateKey);
    //     return cipher.doFinal(content);
    // }

    // public static byte[] decryptByPublicKey(byte[] content, PublicKey publicKey) throws NoSuchAlgorithmException,
    //         NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    //     Cipher cipher = Cipher.getInstance("RSA");
    //     cipher.init(Cipher.DECRYPT_MODE, publicKey);
    //     return cipher.doFinal(content);
    // }

    // 公钥加密bytes
    public static byte[] encryptByPublicKey(byte[] content, PublicKey publicKey) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(content);
    }

    // 私钥解密bytes
    public static byte[] decryptByPrivateKey(byte[] content, PrivateKey privateKey) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
        // Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
        // RSA/ECB/OAEPWithSHA-256AndMGF1Padding
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(content);
    }

    // 获取公钥字符串
    public static String getPublicKeyString(PublicKey publicKey) {
        System.out.println(publicKey.getFormat());
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    // 获取私钥字符串
    public static String getPrivateKeyString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    public static void main(String[] args) throws Exception {
        KeyPair keyPair = generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        System.out.println("public key: " + getPublicKeyString(publicKey));
        System.out.println("private key: " + getPrivateKeyString(privateKey));

        String data = "";
        byte[] encrypted = encryptByPublicKey(data.getBytes(), publicKey);
        System.out.println("encrypted: " + Base64.getEncoder().encodeToString(encrypted));
        // hex
        String hexEncrypted = Hex.toHexString(encrypted);
        System.out.println("hexEncrypted: " + hexEncrypted);
        
        byte[] decrypted = decryptByPrivateKey(encrypted, privateKey);
        System.out.println("decrypted: " + new String(decrypted));


    }
}
