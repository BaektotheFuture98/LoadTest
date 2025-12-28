package dto;

import lombok.Data;

import java.util.ArrayList;

@Data
public class QueryInfo {
    private String time;
    private ArrayList<TestQuery> test_queries;
}
