package dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Data
public class QueryInfo implements Comparable<QueryInfo>{
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String time;
    private transient LocalDateTime timeKey; // 파싱 결과 캐시(직렬화 제외)
    private ArrayList<TestQuery> test_queries;
    public void setTime(String time) {
        this.time = time;
        this.timeKey = (time != null ? LocalDateTime.parse(time, FMT) : null);
    }
    @Override
    public int compareTo(QueryInfo o) {
        if (this.timeKey == null && o.timeKey == null) return 0;
        if (this.timeKey == null) return -1;
        if (o.timeKey == null) return 1;
        return this.timeKey.compareTo(o.timeKey);
    }
}
