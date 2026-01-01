import dto.QueryInfo;
import dto.QueryResult;
import dto.TestQuery;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
class QuerySchedulerTest {

    @Test
    void 부하테스트_전체흐름_검증() throws InterruptedException {
        // 1. 테스트 데이터 준비 (5개 세트)
        List<QueryInfo> query_list = createMockData(5);
        StubElasticsearchService service = new StubElasticsearchService();

        Collections.sort(query_list);
        int listLength = query_list.size();
        int cores = Runtime.getRuntime().availableProcessors();
        int maxThreads = Math.max(1, (int) (cores * 0.5));
        log.info("maxThreads: " + maxThreads);

        // 중요: 메인 스레드가 기다려줄 수 있게 설정
        CountDownLatch countDownLatch = new CountDownLatch(listLength);

        // QueryInfo가 Comparable을 구현했을 때만 작동
        AtomicInteger index = new AtomicInteger(0);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        ExecutorService executor = new ThreadPoolExecutor(1, maxThreads, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        log.info("테스트 시작");

        // 주기적 실행 (테스트를 위해 1분이 아닌 3초 주기로 변경)
        scheduler.scheduleAtFixedRate(() -> {
            int currentIndex = index.getAndIncrement();
            if (currentIndex < listLength) {
                log.info("\n[Scheduler] {}번 인덱스 실행 시작 : {}\n", currentIndex, Thread.currentThread().getName());
                executor.execute(new RunQuery(service, query_list, currentIndex, executor, countDownLatch));
                log.info("[Latch] 카운트 감소: {}\n", countDownLatch.getCount());
            } else {
                log.info(">>> 모든 쿼리가 예약되었습니다. 정리를 시작합니다.");
                scheduler.shutdown();
                executor.shutdown();
            }
        }, 0, 3, TimeUnit.SECONDS); // 3초 주기

        // [중요] 모든 쿼리 세트가 끝날 때까지 최대 2분간 메인 스레드 대기
        boolean finished = countDownLatch.await(1, TimeUnit.MINUTES);

        if (finished) {
            try{
                boolean taskFinished = scheduler.awaitTermination(3, TimeUnit.SECONDS);
                if (taskFinished) {
                    log.info("All schedule have been processed.");
                }else {
                    log.info("All schedule have been processed, but not finished.");
                }
            }catch (InterruptedException e){
                log.info("Await termination interrupted.");
                Thread.currentThread().interrupt();
            }
            try{
                boolean taskFinished = executor.awaitTermination(3, TimeUnit.SECONDS);
                if (taskFinished) {
                    log.info("All queries have been processed.");
                }else {
                    log.info("All queries have been processed, but not finished.");
                }
            }catch (InterruptedException e){
                log.info("Await termination interrupted.");
                Thread.currentThread().interrupt();
            }
            log.info("\n>>> 모든 테스트 작업 완료");
        } else {
            log.info("\n>>> 테스트 타임아웃 종료");
        }
    }

    private static void runQuery(StubElasticsearchService service, List<QueryInfo> testQueries, int currentIndex, Executor executor) {
        try {
            QueryInfo info = testQueries.get(currentIndex);
            List<CompletableFuture<QueryResult>> futures = info.getTest_queries().stream()
                    .map(dto -> CompletableFuture.supplyAsync(() -> {
                        log.info("currentIndex: " + currentIndex + ", dto: " + dto);
                        switch (dto.getKw_command()) {
                            case "search" :
                                return service.getSearchResult("test", dto);
                            case "aggs" :
                                return service.getAggsResult("test", dto);
                            default :
                                throw new IllegalArgumentException("Unknown: " + dto.getKw_command());
                        }
                    }, executor)).toList();

            // 1분 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            System.err.println("쿼리 실행 중 오류 발생: " + e.getMessage());
        }
    }

    private List<QueryInfo> createMockData(int count) {
        List<QueryInfo> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            QueryInfo info = new QueryInfo();
            TestQuery q1 = new TestQuery();
            q1.setId("Q-" + i + "-1"); q1.setKw_command("search"); q1.setQuery("q1 query");
            TestQuery q2 = new TestQuery();
            q2.setId("Q-" + i + "-2"); q2.setKw_command("aggs"); q2.setQuery("q2 query");
            TestQuery q3 = new TestQuery();
            q3.setId("TIMEOUT_TEST"); q3.setKw_command("aggs"); q3.setQuery("q3 query");

            ArrayList<TestQuery> queries = new ArrayList<>();
            queries.add(q1);
            queries.add(q2);
            queries.add(q3);
            info.setTest_queries(queries);
            list.add(info);
        }
        return list;
    }
}