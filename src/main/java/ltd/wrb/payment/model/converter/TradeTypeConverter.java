package ltd.wrb.payment.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ltd.wrb.payment.enums.TradeType;

@Converter(autoApply = true)
public class TradeTypeConverter implements AttributeConverter<TradeType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TradeType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.ordinal();
    }

    @Override
    public TradeType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return TradeType.values()[dbData];
    }
    
}
