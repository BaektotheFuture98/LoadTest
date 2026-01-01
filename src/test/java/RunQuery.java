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
        String threadName = Thread.currentThread().getName();
        try {
            QueryInfo info = testQueries.get(currentIndex);
            // [시작 로그]
            log.info("====> [세트 시작] 인덱스: {} | 스레드: {}", currentIndex, threadName);

            List<CompletableFuture<QueryResult>> futures = info.getTest_queries().stream()
                    .map(dto -> CompletableFuture.supplyAsync(() -> {
                        // [상세 로그] 어떤 쿼리가 돌아가는지
                        log.info("  └─ [상세 실행] 인덱스: {} | 쿼리ID: {} | 명령: {} | 스레드: {}",
                                currentIndex, dto.getId(), dto.getKw_command(), Thread.currentThread().getName());

                        if ("search".equals(dto.getKw_command())) return service.getSearchResult("test", dto);
                        return service.getAggsResult("test", dto);
                    }, executor)).toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } catch (Exception e) {
            log.error(" [!] 에러 발생 (인덱스 {}): {}", currentIndex, e.getMessage());
        } finally {
            latch.countDown();
            // [종료 로그]
            log.info("<==== [세트 완료] 인덱스: {} | 남은 Latch: {}", currentIndex, latch.getCount());
        }
    }
}
