package ltd.wrb.payment.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ltd.wrb.payment.enums.NotifyStatus;

@Converter(autoApply = true)
public class NotifyStatusConverter implements AttributeConverter<NotifyStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(NotifyStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.ordinal();
    }

    @Override
    public NotifyStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return NotifyStatus.values()[dbData];
    }

}
