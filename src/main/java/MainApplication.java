import dto.QueryAtMinuteInterval;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.ElasticsearchRepo;
import io.github.cdimascio.dotenv.Dotenv;
import service.ElasticsearchService;
import util.QueryUtil;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;


public class MainApplication {
    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();
        ElasticsearchRepo repo = new ElasticsearchRepo(dotenv.get("QUETTAI_HOST"), dotenv.get("QUETTAI_USER"), dotenv.get("QUETTAI_PW"));
        ElasticsearchService service = new ElasticsearchService(repo);

        QueryUtil queryUtil = new QueryUtil();
        /// 문제가 되는 시점 2025-10-14 16:00 ~ 2025-10-14 16:04
        /// 분 단위 검색
        String aggs = queryUtil.getAggsQuery("2025-10-14 16:00", "2025-10-14 16:04");
        JSONObject aggsResponse = service.getAggsResult("quetta_logs_2025", aggs);
        JSONArray buckets = aggsResponse.getJSONObject("by_hour").getJSONArray("buckets");


        /// 분 단위별 쿼리 생성
        ArrayList<QueryAtMinuteInterval> dto_list = new ArrayList<>();
        buckets.forEach(bucket -> {
            try {
                QueryAtMinuteInterval dto = new QueryAtMinuteInterval();
                JSONObject bucketResponse = (JSONObject) bucket;
                String date = bucketResponse.getString("key_as_string");
                String search_query = queryUtil.getQuery(date);
                dto.setMinute(date);
                dto.setSearch_query(search_query);
                dto_list.add(dto);
            }catch (Exception e) {
                e.printStackTrace();
            }
        });

        /// sleep (5 min)
        // 부하가 합쳐져있으면 안되기 때문에 분리를 위해 5분 대기



        /// 븐 단위 별 쿼리 실행
        /// 해당 시간대 쿼리 수집
        for(QueryAtMinuteInterval dto : dto_list){
            JSONArray searchResponse = service.getSearchResult("quetta_logs_2025", dto.getSearch_query());
            ArrayList<String> queries = new  ArrayList<>();
            searchResponse.forEach(obj -> {
                JSONObject document = (JSONObject) obj;
                String query = document.getString("at_request_url");
                queries.add(query);
            });
            dto.setQueries(queries.toArray(new String[0]));
        }


        /// 병렬 검색
//        performParallelSearch(dto_list, service, "quetta_logs_2025");
    }

//    private static void performParallelSearch(ArrayList<QueryAtMinuteInterval> dto_list,
//                                              ElasticsearchService service,
//                                              String index) throws Exception {
//        dto_list.stream().map(dto -> CompletableFuture.
//    }



}
