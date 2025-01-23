package ltd.wrb.payment.model;

import lombok.Data;

@Data
public class EVMChainConfigToken {
    
    // {
    //     "address"  :"0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
    //     "decimals" : 6,
    //     "symbol"   : "USDC"
    // }]

    private String address;

    private int decimals;

    private String symbol;
}
