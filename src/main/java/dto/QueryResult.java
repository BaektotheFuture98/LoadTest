package dto;

import lombok.Data;

@Data
public class QueryResult {
    private String id;
    private double duration;
    private boolean time_out;
}
