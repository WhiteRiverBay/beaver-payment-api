package ltd.wrb.payment.controller.dto;

import lombok.Data;
import ltd.wrb.payment.enums.ChainType;

@Data
public class WalletDumpDTO {
    
    private int code;

    private long begin;

    private long end;

    private ChainType chainType;
}
