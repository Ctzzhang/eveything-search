package com.everything.movie.repository.impl;


import com.everything.Redis.RedisUtil;
import com.everything.movie.entity.Movie;
import com.everything.movie.entity.Page;
import com.everything.movie.entity.QueryDTO;
import com.everything.movie.repository.IMovieRepository;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Repository
@Slf4j
public class MovieESRepository implements IMovieRepository {

    public static final String INDEX = "movie";

    public static final String TYPE = "dy2018";
    /*public static final String TYPE2 = "mp4pa";*/

    @Autowired
    private JestClient client;
    @Autowired
    public RedisUtil redis;
    @Override
    public boolean save(Movie movie, String source) {
        Index index = null;
        /*if (source.equals(TYPE)) {*/
            index = new Index.Builder(movie).index(INDEX).type(TYPE).build();
        /*} else  if (source.equals(TYPE2)) {
           index = new Index.Builder(movie).index(INDEX).build();
        }
*/
        try {
            JestResult jestResult = client.execute(index);
            log.info("save返回结果{}", jestResult.getJsonString());
            return jestResult.isSucceeded();
        } catch (IOException e) {
            log.error("save异常", e);
            return false;
        }
    }

    @Override
    public Page<Movie> query(String queryString, int pageNo, int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        HighlightBuilder highlightBuilder = new HighlightBuilder().field("*").requireFieldMatch(false).tagsSchema("default");
        searchSourceBuilder.highlighter(highlightBuilder);
        QueryStringQueryBuilder queryStringQueryBuilder = new QueryStringQueryBuilder(queryString);
        queryStringQueryBuilder
                .field("name", 10)
                .field("translatedName", 8)
                .field("director", 5)
                .field("actor", 3)
                .field("description");
        searchSourceBuilder.query(queryStringQueryBuilder).from(from(pageNo, size)).size(size);
        log.debug("搜索DSL:{}", searchSourceBuilder.toString());


        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX)
                .addType(TYPE)
                .build();
        try {
            SearchResult result = client.execute(search);
            List<SearchResult.Hit<Movie, Void>> hits = result.getHits(Movie.class);
            List<Movie> movies = hits.stream().map(hit -> {
                Movie movie = hit.source;
                Map<String, List<String>> highlight = hit.highlight;
                if (highlight.containsKey("name")) {
                    movie.setName(highlight.get("name").get(0));
                }
                if (highlight.containsKey("translatedName")) {
                    movie.setTranslatedName(highlight.get("translatedName"));
                }
                return movie;
            }).collect(toList());
            int took = result.getJsonObject().get("took").getAsInt();

            Page<Movie> page = Page.<Movie>builder().list(movies).pageNo(pageNo).size(size).total(result.getTotal()).took(took).build();

            return page;
        } catch (IOException e) {
            log.error("search异常", e);
            return null;
        }

    }

    @Override
    public Page<Movie> query(QueryDTO queryDTO, int pageNo, int size) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().from(from(pageNo, size)).size(size);
        if (queryDTO.getMinScore() != null) {
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder("score").gte(queryDTO.getMinScore());
            boolQueryBuilder.must(rangeQueryBuilder);
            searchSourceBuilder.query(boolQueryBuilder);
        }
        if (queryDTO.getOrderBy() != null) {
            searchSourceBuilder.sort(queryDTO.getOrderBy(), SortOrder.DESC);
        }
        log.debug("搜索DSL:{}", searchSourceBuilder.toString());
        Search search = new Search.Builder(searchSourceBuilder.toString())
                .addIndex(INDEX)
                .addType(TYPE)
                .build();
        try {
            SearchResult result = client.execute(search);

            List<Movie> movies = result.getSourceAsObjectList(Movie.class, false);

            int took = result.getJsonObject().get("took").getAsInt();

            //Long a = getTotal(result);
            Long a = result.getTotal();

            Page<Movie> page = Page.<Movie>builder().list(movies).pageNo(pageNo).size(size).total(a).took(took).build();
            return page;
        } catch (IOException e) {
            log.error("search异常", e);
            return null;
        }

    }

    public Long getTotal(SearchResult result) {
        Long total = null;
        JsonElement obj  =  result.getJsonObject();

        JsonElement obj2 =  ((JsonObject) obj).get("hits");
        JsonElement retval = ((JsonObject)obj2).get("total");
        retval=  ((JsonObject) retval).get("value");

        if (obj != retval) total = retval.getAsLong();
        return total;
    }

    @Override
    public Movie get(String id) {
        Get get = new Get.Builder(INDEX, id).type(TYPE).build();
        try {
            JestResult result = client.execute(get);
            Movie movie = result.getSourceAsObject(Movie.class);
            return movie;
        } catch (IOException e) {
            log.error("get异常", e);
            return null;
        }
    }

    @Override
    public boolean deleteAll() {
        Delete.Builder builder = new Delete.Builder(INDEX);
        builder.refresh(true);
        Delete delete = builder.index(INDEX).build();
        JestResult result = null;
        redis.deleteAll();
        try {
            result = client.execute(delete);
        } catch (IOException e) {
            if (result != null && result.isSucceeded()) {
                throw new RuntimeException(result.getErrorMessage()+"删除索引失败!");
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private int from(int pageNo, int size) {
        return (pageNo - 1) * size < 0 ? 0 : (pageNo - 1) * size;
    }
}
