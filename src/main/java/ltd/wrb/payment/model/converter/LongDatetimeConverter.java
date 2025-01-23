package ltd.wrb.payment.model.converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LongDatetimeConverter implements AttributeConverter<Long, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(Long attribute) {
        if (attribute == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(attribute), ZoneOffset.UTC);
    }

    @Override
    public Long convertToEntityAttribute(LocalDateTime dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
