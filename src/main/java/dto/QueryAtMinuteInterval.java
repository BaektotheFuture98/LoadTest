package dto;

import lombok.Data;

@Data
public class QueryAtMinuteInterval {
    private String minute;
    private String search_query;
    private String[] queries;
}
