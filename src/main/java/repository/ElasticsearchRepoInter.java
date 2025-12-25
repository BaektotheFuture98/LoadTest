package repository;

import org.json.JSONArray;
import org.json.JSONObject;


import java.util.Optional;

public interface ElasticsearchRepoInter {
    public JSONObject getSearchResult(String index, String query) throws Exception;
}
