package util;

import dto.QueryResult;
import org.apache.logging.log4j.core.config.Scheduled;
import org.json.JSONArray;

import java.text.ParseException;
import java.util.Calendar;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;


public class DateUtil {
    public String getPlusMin(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime time = LocalDateTime.parse(dateStr, formatter);
        LocalDateTime add_min = time.plusMinutes(1);
        return add_min.toString();
    }
//
}
