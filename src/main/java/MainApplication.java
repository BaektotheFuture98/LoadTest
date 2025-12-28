import dto.QueryInfo;
import dto.QueryResult;
import dto.TestQuery;
import dto.QueryAtMinuteInterval;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.ElasticsearchRepo;
import io.github.cdimascio.dotenv.Dotenv;
import service.ElasticsearchService;
import util.QueryUtil;

import java.util.ArrayList;
import java.util.concurrent.*;


@Slf4j
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
        /// 이걸 보내서 테스트 하는게 아니라 이걸 통해 테스트용 쿼리를 가져올 수 있음
        ArrayList<QueryAtMinuteInterval> aggs_list = new ArrayList<>();
        buckets.forEach(bucket -> {
            try {
                QueryAtMinuteInterval dto = new QueryAtMinuteInterval();
                JSONObject bucketResponse = new JSONObject(bucket);

                String time = bucketResponse.getString("key_as_string");
                dto.setTime(time);

                int doc_count = bucketResponse.getInt("doc_count");
                dto.setDoc_count(doc_count);

                aggs_list.add(dto);
            }catch (Exception e) {
                e.printStackTrace();
            }
        });

        ArrayList<QueryInfo> query_list = new ArrayList<>();
        aggs_list.forEach(dto -> {
            QueryInfo queryInfo = new QueryInfo();
            ArrayList<TestQuery> test_queries = new ArrayList<>();
            try {
                String query = queryUtil.getSearchQuery(dto.getTime());
                JSONArray hitshits = service.getSearchResult("quetta_logs_2025", query);
                hitshits.forEach(obj -> {
                    JSONObject hit = new JSONObject(obj);
                    TestQuery testQuery = new TestQuery();
                    testQuery.setId(hit.getString("_id"));
                    testQuery.setIn_start_date(hit.getString("in_start_date"));
                    testQuery.setIn_start_date(hit.getString("in_end_date"));
                    testQuery.setKw_command(hit.getString("kw_command"));
                    testQuery.setQuery(hit.getString("at_request_url"));
                    test_queries.add(testQuery);
                });
            }catch (Exception e) {
                e.printStackTrace();
            }
            queryInfo.setTime(dto.getTime());
            queryInfo.setTest_queries(test_queries);
            query_list.add(queryInfo);
        });

        /// sleep (5 min)
        // 부하가 합쳐져있으면 안되기 때문에 분리를 위해 1분 대기
        Thread.sleep(60000);

        /// 븐 단위 별 쿼리 실행
        /// 해당 시간대 쿼리 수집
        // 1분마다 하나의 쿼리 묶음들을 병렬로 실행
        // 스레드 단위에서 멀티 스레딩을 해도 될까? => 매우 위험할듯, 어케 조절하지
        // ScheduledThreadPoolExecutor
        // ScheduledExecutorService로 스레드를 실행시킬건데 CompletableFuture.supplyAsnyc()에 ScheduledThreadPoolExecutor를 넣을때도 동일한
        // 결과를 낼 수 있는지 X
        //

        int list_length = query_list.size();
        try(ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(list_length)) {
            // func(runnable, callable), delay, timeunit으로 구성되어 있는데 delay는 timeunit에 따라 단위가 결정된다(분,초 등등)
            ScheduledFuture<?> future = scheduler.schedule(() -> run_query(service, query_list.), 1, TimeUnit.MINUTES);

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void run_query(ElasticsearchService service, ArrayList<TestQuery> testQuery) {
        ArrayList<CompletableFuture<String>> futures = new ArrayList<>();
        for (TestQuery dto : testQuery) {
             // 리스트를 돌건데 return 값을 어떻게 처리하냐...
            CompletableFuture<String> future = CompletableFuture.supplyAsync(()-> {
                switch (dto.getKw_command()) {
                    case "aggs" :
                        return service.getAggsResult("",dto);
                    case "search" :
                        return service.getSearchResult("",dto);
                    default:
                        return null;
                }
            }).thenApply(QueryResult::toString);

            try {
                future.get(10, TimeUnit.MINUTES);
            }catch (Exception e) {
                e.printStackTrace();
            }


            // get() VS join() 비교 필요
        }
    }
}
