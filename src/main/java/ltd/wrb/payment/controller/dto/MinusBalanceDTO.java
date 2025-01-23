package ltd.wrb.payment.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinusBalanceDTO {

    private String uid;
    private String oid;
    private String amount;
    private String remark;

    
}
