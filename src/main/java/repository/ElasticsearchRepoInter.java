package repository;

import org.json.JSONObject;

public interface ElasticsearchRepoInter {
    public JSONObject getSearchResult(String index, String query) throws Exception;
}
