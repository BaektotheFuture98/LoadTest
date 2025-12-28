package dto;

import lombok.Data;

@Data
public class QueryAtMinuteInterval {
    private String time;
    private int doc_count;
}
