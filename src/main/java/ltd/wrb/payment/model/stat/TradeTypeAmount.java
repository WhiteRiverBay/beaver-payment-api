package ltd.wrb.payment.model.stat;

import ltd.wrb.payment.enums.TradeType;

import lombok.Data;

@Data
public class TradeTypeAmount {
    
    private TradeType type;

    private Number amount;
}
