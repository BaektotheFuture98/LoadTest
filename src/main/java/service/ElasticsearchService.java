package service;


import org.json.JSONArray;
import org.json.JSONObject;
import repository.ElasticsearchRepoInter;

import java.util.Optional;

public class ElasticsearchService {
    private final ElasticsearchRepoInter elasticsearchRepo;
    public ElasticsearchService(ElasticsearchRepoInter elasticsearchRepoInter) {
        this.elasticsearchRepo = elasticsearchRepoInter;
    }

    public JSONArray getSearchResult(String index, String query) throws Exception{
        try {
            JSONObject obj = elasticsearchRepo.getSearchResult(index, query);
            return obj.getJSONObject("hits").getJSONArray("hits");
        }catch (Exception e){
            throw new Exception("ElasticsearchService getSearchResult Error");
        }
    }

    public JSONObject getAggsResult(String index, String query) throws Exception{
        try {
            JSONObject obj = elasticsearchRepo.getSearchResult(index, query);
            return obj.getJSONObject("aggregations");
        }catch (Exception e) {
            throw new Exception("ElasticsearchService getAggsResult Error");
        }
    }
}
