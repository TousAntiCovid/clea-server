package fr.gouv.clea.consumer.model;

import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportStat {

    private int reported;

    private int rejected;

    private int backwards;

    private int forwards;

    private int close;

    private long timestamp;

    public ReportStatEntity toEntity() {
        return ReportStatEntity.builder()
                .reported(this.reported)
                .rejected(this.rejected)
                .backwards(this.backwards)
                .forwards(this.forwards)
                .close(this.close)
                .timestamp(TimeUtils.instantFromTimestamp(this.timestamp))
                .build();
    }
}
