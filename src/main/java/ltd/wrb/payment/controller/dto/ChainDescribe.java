package ltd.wrb.payment.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.model.EVMChainConfigToken;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChainDescribe {
    
    private String chainName;

    private String description;

    private ChainType chainType;

    private Integer chainId;

    private Integer confirmBlocksRequired;

    private Integer confirmTimeDelaySeconds;

    private BigDecimal confirmThresholdAmountInDecimals;

    private Map<String, Object> descriptionMap;

    // usdtContracts
    private List<EVMChainConfigToken> usdtContracts;
}
