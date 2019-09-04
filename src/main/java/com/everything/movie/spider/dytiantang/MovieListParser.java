package com.everything.movie.spider.dytiantang;


import com.everything.Redis.RedisUtil;
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

    public static final String START_PAGE = "https://www.dy2018.com/html/gndy/";
    public static final String HTML = ".html";

    public static final String JINGDIAN_PAGE ="jddyy/index";
    private static final String ZUIXIN_PAGE ="dyzz/index";
    private static final String ZONGHE_PAGE ="jddy/index;";


    private static final int PAGE_MAX_NUM = 10;

    public static String[] userAgentList = {
            "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.8) Gecko Fedora/1.9.0.8-1.fc10 Kazehakase/0.5.6",
            "Mozilla/5.0 (X11; Linux i686; U;) Gecko/20070322 Kazehakase/0.4.5",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.71 Safari/537.1 LBBROWSER",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 732; .NET4.0C; .NET4.0E)",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11",
            "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; SV1; QQDownload 732; .NET4.0C; .NET4.0E; 360SE)",
            "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 5.2; .NET CLR 1.1.4322; .NET CLR 2.0.50727; InfoPath.2; .NET CLR 3.0.04506.30)",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.20 (KHTML, like Gecko) Chrome/19.0.1036.7 Safari/535.20",
            "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.8) Gecko Fedora/1.9.0.8-1.fc10 Kazehakase/0.5.6",
            "Mozilla/5.0 (X11; U; Linux x86_64; zh-CN; rv:1.9.2.10) Gecko/20100922 Ubuntu/10.10 (maverick) Firefox/3.6.10",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.71 Safari/537.1 LBBROWSER",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.89 Safari/537.1",
            "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Acoo Browser; SLCC1; .NET CLR 2.0.50727; Media Center PC 5.0; .NET CLR 3.0.04506)",
            "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8.0.12) Gecko/20070731 Ubuntu/dapper-security Firefox/1.5.0.12",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 732; .NET4.0C; .NET4.0E; LBBROWSER)",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.89 Safari/537.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.3 Mobile/14E277 Safari/603.1.30",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36"
    };

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

        String userAgent = userAgentList[new Random().nextInt(userAgentList.length)];
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
                            movieRepository.save(movie);
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