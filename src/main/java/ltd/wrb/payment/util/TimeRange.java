package ltd.wrb.payment.util;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeRange {
    // start
    private LocalDateTime begin;
    // end
    private LocalDateTime end;
}
