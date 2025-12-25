package repository;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Optional;


public class ElasticsearchRepo implements ElasticsearchRepoInter {
    private final RestClient client;

    public ElasticsearchRepo(String hosts, String user, String password) {
        List<String> list = Arrays.asList(hosts.split(","));
        HttpHost[] hostList = list.stream().map( host ->
                new HttpHost(host, 9200, "http")
        ).toArray(HttpHost[]::new);

        String CREDENTIALS_STRING = user + ":" + password;
        String encodedBytes = Base64.getEncoder().encodeToString(CREDENTIALS_STRING.getBytes());
        Header[] headers = {
                new BasicHeader("Authorization", "Basic " + encodedBytes)
        };
        client = RestClient.builder(hostList)
                .setDefaultHeaders(headers)
                .build();
    }

    @Override
    public JSONObject getSearchResult(String index, String query) throws Exception {
        try {
            Request request = new Request("GET", index+"/_search");
            request.setEntity(new NStringEntity(query, ContentType.APPLICATION_JSON));
            Response response = client.performRequest(request);
            JSONObject obj = new JSONObject(EntityUtils.toString(response.getEntity()));
            return obj;
        }catch (Exception e){
            throw new Exception("ElasticsearchRepo getSearchResult Error");
        }
    }
}
