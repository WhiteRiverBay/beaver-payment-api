package ltd.wrb.payment.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ltd.wrb.payment.enums.ChainType;

@Converter(autoApply = true)
public class ChainTypeConverter implements AttributeConverter<ChainType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ChainType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.ordinal();
    }

    @Override
    public ChainType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return ChainType.values()[dbData];
    }

}
