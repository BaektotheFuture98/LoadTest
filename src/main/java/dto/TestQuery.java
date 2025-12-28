package dto;

import lombok.Data;

@Data
public class TestQuery {
    private String id; // quetta_logs_2025 기준 id
    private String query; // body
    private String dt_reg_date; // 실행 시간대
    private String in_start_date; // 검색 시간대 - start
    private String in_end_date;  // 검색 시간대 - end
    private String kw_command; // search or aggs
}
