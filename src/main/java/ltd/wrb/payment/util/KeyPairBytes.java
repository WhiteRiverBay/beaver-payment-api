package ltd.wrb.payment.util;

import org.bouncycastle.util.encoders.Hex;

import cn.hutool.core.codec.Base58;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeyPairBytes {
    
    private byte[] privateKey;
    private byte[] publicKey;

    public String getPrivateKeyHex() {
        return Hex.toHexString(privateKey);
    }

    public String getPublicKeyHex() {
        return Hex.toHexString(publicKey);
    }

    public String getAddress() {
        return Base58.encode(publicKey);
    }
}
