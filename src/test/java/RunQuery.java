import dto.QueryInfo;
import dto.QueryResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;


@Slf4j
public class RunQuery implements Runnable {
    StubElasticsearchService service;
    List<QueryInfo> testQueries;
    int currentIndex;
    Executor executor;
    CountDownLatch latch;
    public RunQuery(StubElasticsearchService service, List<QueryInfo> testQueries, int currentIndex, Executor executor, CountDownLatch latch) {
        this.service = service;
        this.testQueries = testQueries;
        this.currentIndex = currentIndex;
        this.executor = executor;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            QueryInfo info = testQueries.get(currentIndex);
            log.info("currentIndex: " + currentIndex + ", info: " + info);
            List<CompletableFuture<QueryResult>> futures = info.getTest_queries().stream()
                    .map(dto -> CompletableFuture.supplyAsync(() -> {
                        log.info("currentIndex: " + currentIndex + ", dto: " + dto);
                        switch (dto.getKw_command()) {
                            case "search":
                                return service.getSearchResult("test", dto);
                            case "aggs":
                                return service.getAggsResult("test", dto);
                            default:
                                throw new IllegalArgumentException("Unknown: " + dto.getKw_command());
                        }
                    }, executor)).toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("쿼리 실행 중 오류 발생: " + e.getMessage());
        }finally {
            latch.countDown();
        }
    }
}
