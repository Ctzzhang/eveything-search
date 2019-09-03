package com.everything.redis;

import com.everything.Redis.RedisUtil;
import com.everything.movie.entity.Movie;
import com.everything.movie.spider.MovieDetailParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class RedisTeat {
    @Autowired
    private RedisUtil redisUtil;

    @Test
    public void testParse() throws IOException {

        log.info("电影详情:{}");
        redisUtil.set("qqq","bbbb");
        Assert.assertEquals(redisUtil.get("qqq"),"bbbb");
    }
}
