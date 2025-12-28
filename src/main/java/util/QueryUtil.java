package util;

import java.text.ParseException;
import java.util.Date;

public class QueryUtil {

    public String getSearchQuery(String date) {
        DateUtil dateUtil = new DateUtil();
        String plusMin = dateUtil.getPlusMin(date);
        return "{\n" +
                "  \"query\": {\n" +
                "    \"range\": {\n" +
                "      \"dt_reg_date\": {\n" +
                "        \"gte\": \""+date+"\",\n" +
                "        \"lt\":  \""+plusMin+"\",\n" +
                "        \"format\": \"yyyy-MM-dd HH:mm\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getAggsQuery(String start_date, String end_date) {
        return "{\n" +
                "  \"size\": 0,\n" +
                "  \"query\": {\n" +
                "    \"range\": {\n" +
                "      \"dt_reg_date\": {\n" +
                "        \"gte\": \""+start_date+"\",\n" +
                "        \"lt\":  \""+end_date+"\",\n" +
                "        \"format\": \"yyyy-MM-dd HH:mm\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"aggs\": {\n" +
                "    \"by_hour\": {\n" +
                "      \"date_histogram\": {\n" +
                "        \"field\": \"dt_reg_date\",\n" +
                "        \"calendar_interval\": \"1m\",\n" +
                "        \"min_doc_count\": 0,\n" +
                "        \"extended_bounds\": {\n" +
                "          \"min\": \""+start_date+"\",\n" +
                "          \"max\": \""+end_date+"\"\n" +
                "        },\n" +
                "        \"format\": \"yyyy-MM-dd HH:mm\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

}
