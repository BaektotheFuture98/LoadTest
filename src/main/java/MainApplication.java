import dto.QueryAtMinuteInterval;
import dto.QueryInfo;
import dto.QueryResult;
import dto.TestQuery;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.ElasticsearchRepo;
import service.ElasticsearchService;
import util.QueryUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


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

        /// 분 단위 별 쿼리 실행
        try {
            int listLength = query_list.size();

            int cores = Runtime.getRuntime().availableProcessors();
            int maxThreads = Math.max(1, (int) (cores * 0.5));

            Collections.sort(query_list);
            AtomicInteger index = new AtomicInteger(0);

            ScheduledExecutorService scheduler =
                    Executors.newScheduledThreadPool(3);  // DelayedWorkQueue()를 쓰기 때문에 executor을 새로 생성
            ExecutorService executor = new ThreadPoolExecutor(
                    1, maxThreads, 3, TimeUnit.SECONDS, new LinkedBlockingQueue<>()
            );

            scheduler.scheduleAtFixedRate(() -> {
                int currentIndex = index.getAndIncrement();
                if (currentIndex < listLength) {
                    List<QueryResult> results = runQuery(service, query_list, currentIndex, executor);
                    results.forEach(result -> {
                        System.out.println(result.toString());
                    });
                }else{
                    System.out.println("All queries have been processed.");
                    scheduler.shutdown();
                    executor.shutdown();
                }
            }, 0, 1, TimeUnit.MINUTES);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<QueryResult> runQuery(
            ElasticsearchService service,
            List<QueryInfo> testQueries,
            int currentIndex,
            Executor executor
    ) {
        QueryInfo info = testQueries.get(currentIndex);
        List<CompletableFuture<QueryResult>> futures = new ArrayList<>();
        info.getTest_queries().forEach(testQuery -> {
            CompletableFuture<QueryResult> result = CompletableFuture.supplyAsync(() -> {
                switch (testQuery.getKw_command()) {
                    case "aggs":
                        return service.getAggsResult("", testQuery);
                    case "search":
                        return service.getSearchResult("", testQuery);
                    default:
                        throw new IllegalArgumentException("Unknown command");
                }
            }, executor);
            futures.add(result);
        });

        // 모든 쿼리 완료 대기
        CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        ).join();

        // 결과 수집 (thread-safe)
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
}