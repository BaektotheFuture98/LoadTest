import dto.QueryResult;
import dto.TestQuery;

public class StubElasticsearchService {

    /**
     * Search 쿼리 시뮬레이션
     */
    public QueryResult getSearchResult(String index, TestQuery dto) {
        simulateDelay(dto.getId()); // ID에 따라 지연 시간 조절
        System.out.printf("[ES-Stub] Search 실행 완료 - ID: %s, Query: %s\n", dto.getId(), dto.getQuery());

        QueryResult result = new QueryResult();
        System.out.println(dto.toString());
        // 필요 시 result.setSuccess(true) 등의 데이터 세팅
        return result;
    }

    /**
     * Aggs 쿼리 시뮬레이션
     */
    public QueryResult getAggsResult(String index, TestQuery testQuery) {
        simulateDelay(testQuery.getId());
        System.out.printf("[ES-Stub] Aggs 실행 완료 - ID: %s, Command: %s\n", testQuery.getId(), testQuery.getKw_command());
        QueryResult dto = new QueryResult();
        dto.setId(testQuery.getId());
        dto.setDuration(100);
        dto.setTime_out(false);
        System.out.println(dto.toString());
        return dto;
    }

    /**
     * 테스트를 위한 인위적인 지연 발생기
     */
    private void simulateDelay(String id) {
        try {
            // 특정 ID(예: "TIMEOUT")일 경우 1분 넘게 대기시켜 타임아웃 테스트 가능
            if ("TIMEOUT_TEST".equals(id)) {
                Thread.sleep(10000); // 10초 대기 (1분 초과)
            } else {
                // 일반적인 경우 100~1000ms 사이 랜덤 지연
                Thread.sleep((long) (Math.random() * 3900) + 100);
            }
        } catch (InterruptedException e) {
            // cancel(true) 호출 시 여기서 인터럽트 발생
            System.out.printf("[ES-Stub] %s 작업이 인터럽트(취소)되었습니다.\n", id);
            Thread.currentThread().interrupt();
        }
    }
}