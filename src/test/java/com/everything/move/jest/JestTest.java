package com.everything.move.jest;

import com.everything.movie.entity.Movie;
import com.everything.movie.spider.MovieDetailParser;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.mapping.PutMapping;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class JestTest {

    @Autowired
    private JestClient client;

    @Test
    public void testDeleteIndex() throws IOException {
        JestResult jestResult = client.execute(new DeleteIndex.Builder("articles").build());
        log.info("testDeleteIndex返回结果{}", jestResult.getJsonString());
        Assert.assertNotNull(jestResult);

    }

    @Test
    public void testCreateIndex() throws IOException {
        CreateIndex createIndex = new CreateIndex.Builder("articles").build();
        JestResult jestResult = client.execute(createIndex);
        log.info("testCreateIndex返回结果{}", jestResult.getJsonString());
        Assert.assertNotNull(jestResult);
    }

    @Test
    public void testMapping() throws IOException {
        testCreateIndex();
        PutMapping putMapping = new PutMapping.Builder(
                "articles",
                "tweet",
                "{ \"tweet\" : { \"properties\" : { \"date\" : {\"type\" : \"date\", \"store\" : \"yes\"} } } }"
        ).build();
        JestResult jestResult = client.execute(putMapping);
        log.info("testMapping返回结果{}", jestResult.getJsonString());
        Assert.assertTrue(jestResult.isSucceeded());
    }

    @Test
    public void testIndex() throws IOException {
        Article source = Article.builder().author("cbwleft").content("learning elasticsearch is a long trip").date(new Date()).build();
        Index index = new Index.Builder(source).index("articles").type("tweet").build();
        JestResult jestResult = client.execute(index);
        log.info("testIndex返回结果{}", jestResult.getJsonString());
        Assert.assertTrue(jestResult.isSucceeded());

        source = Article.builder().author("elastic").content("elastic权威指南").id("1").date(new Date()).build();
        index = new Index.Builder(source).index("articles").type("tweet").build();
        jestResult = client.execute(index);
        log.info("testIndex返回结果{}", jestResult.getJsonString());
        Assert.assertTrue(jestResult.isSucceeded());
    }

    @Test
    public void testSearch() throws IOException {
        //Add ElasticSearch dependency to use SearchSourceBuilder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termsQuery("author", "cbwleft"));

        Search search = new Search.Builder(searchSourceBuilder.toString())
                // multiple index or types can be added.
                .addIndex("articles")
                .addType("tweet")
                .build();

        SearchResult result = client.execute(search);
        List<SearchResult.Hit<Article, Void>> hits = result.getHits(Article.class);
        log.info("testSearch返回{}", hits.size());
        hits.forEach(hit -> log.info("{}", hit.source));
        Assert.assertNotNull(hits);
    }

    @Autowired
    private MovieDetailParser movieDetail;

    @Test
    public void testMovie() throws IOException {
        Movie movie = movieDetail.parse("100061");
        Index index = new Index.Builder(movie).index("movie").type("dy2018").build();
        JestResult jestResult = client.execute(index);
        Assert.assertTrue(jestResult.isSucceeded());
    }

}
