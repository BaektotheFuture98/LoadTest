package service;


import dto.QueryResult;
import dto.TestQuery;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.ElasticsearchRepoInter;


public class ElasticsearchService {
    private final ElasticsearchRepoInter elasticsearchRepo;
    public ElasticsearchService(ElasticsearchRepoInter elasticsearchRepoInter) {
        this.elasticsearchRepo = elasticsearchRepoInter;
    }

    public JSONArray getSearchResult(String index, String query){
        try {
            JSONObject obj = elasticsearchRepo.getSearchResult(index, query);
            return obj.getJSONObject("hits").getJSONArray("hits");
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public QueryResult getSearchResult(String index, TestQuery testQuery){
        try {
            QueryResult dto = new QueryResult();
            JSONObject obj = elasticsearchRepo.getSearchResult(index, testQuery.getQuery());
            dto.setId(testQuery.getId());
            dto.setDuration(obj.getInt("took"));
            dto.setTime_out(obj.getBoolean("timed_out"));
            return dto;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject getAggsResult(String index, String query) {
        try {
            JSONObject obj = elasticsearchRepo.getSearchResult(index, query);
            return obj.getJSONObject("aggregations");
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public QueryResult getAggsResult(String index, TestQuery testQuery){
        try {
            QueryResult dto = new QueryResult();
            JSONObject obj = elasticsearchRepo.getSearchResult(index, testQuery.getQuery());
            dto.setId(testQuery.getId());
            dto.setDuration(obj.getInt("took"));
            dto.setTime_out(obj.getBoolean("timed_out"));
            return dto;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
