import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CompletableFutureTest {
    @DisplayName("Executor Test")
    void parallel(){
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
        executor.schedule()
        CompletableFuture<String> test = CompletableFuture.supplyAsync(this::test, executor);
        test.join();
    }

    private String test(){
        System.out.printf("[%s]\n", Thread.currentThread().getName());
        return "test";
    }

}
