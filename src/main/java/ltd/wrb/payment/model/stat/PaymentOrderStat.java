package ltd.wrb.payment.model.stat;

import lombok.Data;
import ltd.wrb.payment.enums.TradeStatus;

@Data
public class PaymentOrderStat {
    
    private TradeStatus status;

    private Number count;

    private Number sumAmount;

    private Number avgAmount;

    private Number maxAmount;
}
