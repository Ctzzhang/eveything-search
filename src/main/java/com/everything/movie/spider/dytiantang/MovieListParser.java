package com.everything.movie.spider.dytiantang;


import com.everything.Redis.RedisUtil;
import com.everything.movie.common.MovirConstants;
import com.everything.movie.entity.Movie;
import com.everything.movie.repository.IMovieRepository;
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
public class MovieListParser {


    @Autowired
    public RedisUtil redis;
    public static final String SOURCE = "dy2018";
    public static final String START_PAGE = "https://www.dy2018.com/html/gndy/";
    public static final String HTML = ".html";

    public static final String JINGDIAN_PAGE ="jddyy/index";
    private static final String ZUIXIN_PAGE ="dyzz/index";
    private static final String ZONGHE_PAGE ="jddy/index;";


    private static final int PAGE_MAX_NUM = 10;


    @Autowired
    private MovieDetailParser movieDetailParser;

    @Autowired
    private IMovieRepository movieRepository;

    private Set<String> ids = new HashSet<>();

    public void parse(String url) throws IOException {
        if (redis.sHasKey(MovieListParser.START_PAGE+HTML, url)) {
            log.info("redis has url {}", url);
            while (redis.sHasKey(MovieListParser.START_PAGE+HTML, url)) {
                url = getNextUrl(url);
                if (null == url) {
                    log.info("所有页面抓全完成");
                    return;
                }
            }
            parse(url);
        }

        String userAgent = MovirConstants.userAgentList[new Random().nextInt(MovirConstants.userAgentList.length)];
        log.info("抓取列表页面:{}", url);

       // Document document = Jsoup.connect(url).userAgent(userAgent).timeout(10000).get();

        Connection conn = Jsoup.connect(url).userAgent(userAgent).timeout(50000);
        conn.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        conn.header("Accept-Encoding", "gzip, deflate, sdch");
        conn.header("Accept-Language", "zh-CN,zh;q=0.8");

        Document document = null;
        try {
            document =  conn.get();
        } catch (IOException e) {
            log.error("获取{}页面异常:{}", url, e);
            url = getNextUrl(url);
            if (null == url) {
                log.info("所有页面抓全完成");
                return;
            }
            while (redis.sHasKey(MovieListParser.START_PAGE + HTML, url)) {
                url = getNextUrl(url);
                if (null == url) {
                    log.info("所有页面抓全完成");
                    return;
                }
            }
            parse(url);

        }

        boolean allMovieId = true;



        for (Element a : document.select(".co_content8 a")) {
            String href = a.attr("href");
            if (href.matches("/i/[0-9]+.html")) {
                String id = href.substring(3, href.lastIndexOf("."));
                if (ids.contains(id)) {
                    log.info("已经抓取过该电影，不再重复抓取");
                } else {
                    log.info("开始抓取电影id:{}", id);
                    try {
                        Thread.sleep(1000);
                        Movie movie = movieDetailParser.parse(id);
                        if (null != movie) {
                            movieRepository.save(movie, SOURCE);
                            ids.add(id);
                            redis.sSet(START_PAGE, id);
                        }

                    } catch (IOException e) {
                        allMovieId = false;

                        log.error("抓取电影id:{}异常", id, e);
                    } catch (InterruptedException e) {
                        allMovieId = false;
                    }
                }
            }
        }
        if (allMovieId) {
            redis.sSet(START_PAGE+HTML, url);
        }



        document.select("div.x a").forEach(a -> {
            String text = a.text();

            if (text.equals("下一页") ) {
                try {
                    parse(a.absUrl("href"));//recursion


                } catch (IOException e) {
                    log.error("抓取下一页异常:{}", e);
                }
            }
        });
    }

    public String getNextUrl(String url) {
        String[] htmlStr = url.split("_");
        String[] m = htmlStr[htmlStr.length - 1].split("\\.");
        if (url.equals(START_PAGE+ JINGDIAN_PAGE + HTML)) {
            url = START_PAGE+ JINGDIAN_PAGE + "_2" + HTML;
        } else if (htmlStr[0].equals(START_PAGE + JINGDIAN_PAGE )) {

            int pageNum = Integer.parseInt(m[0]);
            if (pageNum > PAGE_MAX_NUM) {
                url = START_PAGE+ZUIXIN_PAGE + HTML;
            } else {
                url = START_PAGE+JINGDIAN_PAGE + "_" + (pageNum + 1) + HTML;
            }
        } else  if (url.equals(START_PAGE+ZUIXIN_PAGE + HTML)) {
            url = START_PAGE+ZUIXIN_PAGE + "_2" + HTML;
        } else if (htmlStr[0].equals(START_PAGE+ZUIXIN_PAGE )) {
            int pageNum = Integer.parseInt(m[0]);
            if (pageNum > PAGE_MAX_NUM) {
                url = START_PAGE+ZONGHE_PAGE + HTML;
            } else {
                url = START_PAGE+ ZUIXIN_PAGE + "_" + (pageNum + 1) + HTML;
            }
        } else  if (url.equals(START_PAGE+ZONGHE_PAGE + HTML)) {
            url = START_PAGE+ZONGHE_PAGE + "_2" + HTML;
        } else if (htmlStr[0].equals(START_PAGE+ZONGHE_PAGE )) {
            int pageNum = Integer.parseInt(m[0]);
            if (pageNum > PAGE_MAX_NUM) {
                return null;
            } else {
                url = START_PAGE+ ZONGHE_PAGE + "_" + (pageNum + 1) + HTML;
            }
        }
         return url;
    }
}
