package ltd.wrb.payment.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ltd.wrb.payment.enums.TradeStatus;

@Converter(autoApply = true)
public class TradeStatusConverter implements AttributeConverter<TradeStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TradeStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.ordinal();
    }

    @Override
    public TradeStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return TradeStatus.values()[dbData];
    }
    
}
