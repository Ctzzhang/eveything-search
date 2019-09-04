package com.everything.movie.spider.mp4Ba;


import com.everything.Redis.RedisUtil;
import com.everything.movie.common.MovirConstants;
import com.everything.movie.entity.Movie;
import com.everything.movie.repository.IMovieRepository;
import com.everything.movie.spider.dytiantang.MovieDetailParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Component
@Slf4j
public class Mp4ListParser {


    private static Mp4ListParser mp4DetailParser;
    @Autowired
    public RedisUtil redis;

    public static final String START_PAGE = "https://www.mp4pa.com/dy/hd1.html";
    public static final String HTML = ".html";



    private static final int PAGE_MAX_NUM = 10;




    private Set<String> ids = new HashSet<>();

    public static void parse(String url) throws IOException {
        String userAgent = MovirConstants.userAgentList[new Random().nextInt(MovirConstants.userAgentList.length)];
        // Document document = Jsoup.connect(url).userAgent(userAgent).timeout(10000).get();
        log.info("抓取列表页面:{}", url);
        Connection conn = Jsoup.connect(url).userAgent(userAgent).timeout(50000);
        conn.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        conn.header("Accept-Encoding", "gzip, deflate, sdch");
        conn.header("Accept-Language", "zh-CN,zh;q=0.8");
        Document document = null;
        try {
            document =  conn.get();
        } catch (IOException e) {
            log.error("获取{}页面异常:{}", url, e);

        }


        for (Element a : document.select(".weixin")) {
            String href = a.getElementsByTag("a").attr("href");
            if (href.matches("/hddy/hdmp[0-9]+-0-0.html")) {
                String id = href.substring(6, href.lastIndexOf("."));
                log.info("电影的id是{}", id);
                Mp4DetailParser.parse(id);
                }
            }
        }





}