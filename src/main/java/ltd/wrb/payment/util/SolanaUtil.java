package ltd.wrb.payment.util;

import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

public class SolanaUtil {
    // create keypair
    public static KeyPairBytes createKeyPair() {
        // KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
        // ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        // keyPairGenerator.initialize(ecSpec, new SecureRandom());
        // KeyPair keyPair = keyPairGenerator.generateKeyPair();
        // return keyPair;

        Ed25519KeyPairGenerator keyPairGenerator = new Ed25519KeyPairGenerator();
        keyPairGenerator.init(new KeyGenerationParameters(new SecureRandom(), 256));

        // 生成密钥对
        AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();

        // 获取私钥和公钥
        Ed25519PrivateKeyParameters privateKeyParams = (Ed25519PrivateKeyParameters) keyPair.getPrivate();
        Ed25519PublicKeyParameters publicKeyParams = (Ed25519PublicKeyParameters) keyPair.getPublic();

        byte[] privateKey = privateKeyParams.getEncoded();
        byte[] publicKey = publicKeyParams.getEncoded();

        return KeyPairBytes.builder().privateKey(privateKey).publicKey(publicKey).build();
    }
}
