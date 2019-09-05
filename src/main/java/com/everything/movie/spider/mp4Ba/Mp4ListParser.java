package com.everything.movie.spider.mp4Ba;


import com.everything.Redis.RedisUtil;
import com.everything.movie.common.MovirConstants;
import com.everything.movie.entity.Movie;
import com.everything.movie.repository.IMovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Component
@Slf4j
public class Mp4ListParser {

    public static final String SOURCE = "mp4pa";

    @Autowired
    public RedisUtil redis;
    @Autowired
    public IMovieRepository movieRepository;
    @Autowired
    public Mp4DetailParser mp4DetailParser;
    public static final  String ALL_PAGE = "https://www.mp4pa.com";
    public static final String START_PAGE = "/dy/hd1.html";
    public static final String HTML = ".html";


    public static String nowPageHtml = "";

    private static final int PAGE_MAX_NUM = 10;


    private Set<String> ids = new HashSet<>();

    public void parse(String url) throws IOException {
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
            parse(url);
        }


        for (Element a : document.select(".weixin")) {
            String href = a.getElementsByTag("a").attr("href");
            if (href.matches("/hddy/hdmp[0-9]+-0-0.html")) {
                String id = href.substring(6, href.lastIndexOf("."));
                log.info("电影的id是{}", id);
                Movie movie = mp4DetailParser.parse(id);
                if (null != movie) {
                    movieRepository.save(movie, SOURCE);
                    ids.add(id);
                }
            }
        }

        //获取下一页
        Elements eles = document.getElementsByClass("pagination pagination-lg hidden-xs page");
        Element s = eles.get(0);
        String h = "";
        for (Element seles : s.getAllElements()) {
            h = seles.getElementsByTag("a").attr("href");
            if (h.isEmpty() || h.equals(" ")) {
                continue;
            }
            log.debug(h);
            if (nowPageHtml.equals("") || h.compareTo(nowPageHtml) == 1 || h.length() > nowPageHtml.length())  {
                 break;
            }
        }
        if (nowPageHtml.equals(h) || h.equals("") ) {
            log.info("抓取完毕{}", nowPageHtml);
            return;
        }
        nowPageHtml = h;

        try {
            log.info("nextUrl{}", nowPageHtml);
            parse(ALL_PAGE + nowPageHtml);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("nextUrl{} erro", nowPageHtml, e);

        }
    }

}
