import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class CompletableFutureTest {
    @Test
    public void givenJoinMethod_whenThrow_thenGetUncheckedException() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Test join");

        assertEquals("Test join", future.join());

        CompletableFuture<String> exceptionFuture = CompletableFuture.failedFuture(new RuntimeException("Test join exception"));

        assertThrows(CompletionException.class, exceptionFuture::join);
    }

    @Test
    public void givenGetMethod_whenThrow_thenGetCheckedException() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Test get");

        try {
            assertEquals("Test get", future.get());
        } catch (InterruptedException | ExecutionException e) {
            fail("Exception should not be thrown");
        }

        CompletableFuture<String> exceptionFuture = CompletableFuture.failedFuture(new RuntimeException("Test get exception"));

        assertThrows(ExecutionException.class, exceptionFuture::get);
    }
}
