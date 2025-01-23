package ltd.wrb.payment.util;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;

import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.web3j.crypto.Keys;

import cn.hutool.core.codec.Base58;
import ltd.wrb.payment.util.crypto.ECKey;

public class TRONUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    static public class TKeyPair {

        final public byte[] privateKey;
        final public byte[] publicKey;

        TKeyPair(byte[] privateKey, byte[] publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }
    }

    public static TKeyPair createKeyPair() throws Exception {
        // KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        // ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        // keyPairGenerator.initialize(ecSpec, new SecureRandom());
        // KeyPair keyPair = keyPairGenerator.generateKeyPair();
        // return keyPair;

        ECKey key = new ECKey(new SecureRandom());
        byte[] privateKey = key.getPrivateKey();
        byte[] publicKey = key.getPubKey();
        return new TKeyPair(privateKey, publicKey);
    }

    public static void importHexPrivateKey(String privateKeyHex) throws Exception {
        byte[] privateKeyBytes = Hex.decode(privateKeyHex);
        ECKey key = ECKey.fromPrivate(privateKeyBytes);

        byte[] hexAddress = key.getAddress();
        String address = encodeBase58Check(hexAddress);
        System.out.println("Address after import: " + address);
    }

    public static String getPrivateKeyHex(PrivateKey privateKey) {
        return Hex.toHexString(privateKey.getEncoded());
    }

    public static String getPublicKeyHex(PublicKey publicKey) {
        return Hex.toHexString(publicKey.getEncoded());
    }

    public static String getTronAddress(byte[] pubKeyBytes) {
        // byte[] pubKeyBytes = publicKey.getEncoded();

        // SHA3.Digest256 sha3 = new SHA3.Digest256();
        Keccak.Digest256 sha3 = new Keccak.Digest256();
        // byte[] hash = sha3.digest(pubKeyBytes);
        byte[] hash = sha3.digest(Arrays.copyOfRange(pubKeyBytes, 1, pubKeyBytes.length));

        byte[] addressBytes = Arrays.copyOfRange(hash, hash.length - 20, hash.length);

        byte[] tronAddressBytes = new byte[1];
        tronAddressBytes[0] = 0x41;
        String tronAddress = encodeBase58Check(Arrays.concatenate(tronAddressBytes, addressBytes));
        return tronAddress;
    }

    public static String toTronAddress(String ethereumAddress) {
        String address = ethereumAddress.startsWith("0x") ? ethereumAddress.substring(2) : ethereumAddress;
        byte[] addressBytes = Hex.decode(address);

        byte[] tronAddressBytes = new byte[1];
        tronAddressBytes[0] = 0x41;

        String tronAddress = encodeBase58Check(Arrays.concatenate(tronAddressBytes, addressBytes));
        return tronAddress;
    }

    public static String toETHAddress(String tronAddress) {
        byte[] decodedBytes = Base58.decode(tronAddress);
        // 去掉前1个字节和最后4个字节
        byte[] truncatedBytes = Arrays.copyOfRange(decodedBytes, 1, decodedBytes.length - 4);
        // 转换为16进制字符串
        String ethereumAddress = new BigInteger(1, truncatedBytes).toString(16);
        // 确保字符串长度为40个字符，不足时补0
        while (ethereumAddress.length() < 40) {
            ethereumAddress = "0" + ethereumAddress;
        }
        String address = "0x" + ethereumAddress;
        return Keys.toChecksumAddress(address);
    }

    public static void main(String[] args) {

        // 生成
        try {
            TKeyPair keyPair = createKeyPair();
            String privateKeyHex = Hex.toHexString(keyPair.privateKey);
            String publicKeyHex = Hex.toHexString(keyPair.publicKey);
            System.out.println("Private Key: " + privateKeyHex);
            System.out.println("Public Key: " + publicKeyHex);
            String tronAddress = getTronAddress(keyPair.publicKey);
            System.out.println("Tron Address: " + tronAddress);
            importHexPrivateKey(privateKeyHex);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String encodeBase58Check(byte[] input) {
        byte[] hash = doubleSha256(input);
        byte[] checksum = Arrays.copyOfRange(hash, 0, 4);
        byte[] tronAddressWithChecksum = Arrays.concatenate(input, checksum);
        // return Base58.encode(tronAddressWithChecksum);
        return Base58.encode(tronAddressWithChecksum);
    }

    private static byte[] doubleSha256(byte[] input) {
        // SHA3.Digest256 sha3 = new SHA3.Digest256();
        SHA256.Digest sha3 = new SHA256.Digest();
        byte[] firstHash = sha3.digest(input);
        return sha3.digest(firstHash);
    }

}
