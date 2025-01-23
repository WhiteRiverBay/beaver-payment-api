package ltd.wrb.payment.controller.dto;

import lombok.Data;
import ltd.wrb.payment.enums.ChainType;

@Data
public class ExceptionHashDTO {
    private String hash;
    private ChainType chainType;
    private Integer chainId;

    public boolean isValid() {
        if (hash == null || chainType == null) {
            return false;
        }
        if (chainType == ChainType.EVM) {
            return chainId != null && chainId >= 0;
        }
        if (chainId < 0) {
            return false;
        }
        String ahash = hash.startsWith("0x") ? hash.substring(2) : hash;
        return ahash.matches("^[0-9a-fA-F]+$");
    }
}
