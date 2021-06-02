package fr.gouv.clea.ws.model;

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
}
