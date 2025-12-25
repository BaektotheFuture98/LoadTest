package util;

import java.text.ParseException;
import java.util.Calendar;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;


public class DateUtil {
    public String getPlusMin(String dateStr) throws ParseException {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime time = LocalDateTime.parse(dateStr, formatter);
            LocalDateTime add_min = time.plusMinutes(1);
            return add_min.toString();
        } catch (Exception e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }
}
