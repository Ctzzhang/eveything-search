package com.everything.movie.spider.mp4Ba;

import com.everything.Redis.RedisUtil;
import com.everything.movie.entity.Movie;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.everything.movie.spider.dytiantang.MovieListParser.userAgentList;
import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class Mp4DetailParser {


    @Autowired
    public RedisUtil redis;
    public static final String URL_PATTERN = "https://www.mp4pa.com/hddy/{0}.html";
    private static String REGEX_CHINESE = "[\u4e00-\u9fa5]";// 中文正则

    public static Movie parse(String id) throws IOException {
        /*if (redis.sHasKey(MovieListParser.START_PAGE, id)) {
            log.info("redis has id {}", id);
            return null;
        }*/

        String userAgent = userAgentList[new Random().nextInt(userAgentList.length)];
        String url = MessageFormat.format(URL_PATTERN, id);
        Document document = Jsoup.connect(url).userAgent(userAgent).timeout(100000).get();
        Movie movie = new Movie();
        movie.setId(id);

        Elements ab = document.getElementsByClass("col-md-17  sea-col");
        Element ab0 = ab.get(0);
        Element ab1 = ab.get(1);
        Elements ab1list = ab1.getAllElements();

        String title = ab1list.get(2).text();
        log.debug("标题:{}", title);
        movie.setTitle(title);


        String score = "0";
        log.debug("评分:{}", score);
        try {
            movie.setScore(Float.parseFloat(score));
        } catch (NumberFormatException e) {
            log.info(e.getMessage());
        }
        /*log.debug("发布日期:{}", ab1list.get(9).text().split(":")[1].split("\\(")[0].replace(" ", ""));
        String updateDate = ab1list.get(9).text().split(":")[1].split("\\(")[0].replace(" ", "");;;
        log.debug("发布日期:{}", updateDate);
        try {
            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            updateDate = updateDate.replace("发布时间：", "");
            movie.setUpdateDate(sdf.parse(updateDate));
            movie.setUpdateDay(LocalDate.parse(updateDate, dtf));
        } catch (ParseException e) {
            log.info("发布日期{}解析出错", updateDate);
        }*/
        Elements zoom = document.select("#Zoom");
        try {
            String coverUrl = ab.get(0).select("img").attr("src");
            log.debug("封面图片:{}", coverUrl);
            movie.setCoverUrl(coverUrl);
        } catch (Exception e) {
            log.info("封面图片解析出错");
        }
        List<String> actor = new ArrayList<>();
        movie.setActor(actor);
        for (int i = 0; i< ab1.getAllElements().size(); i++) {
            if (i == 0) {
                continue;
            }
            String text = ab1.getAllElements().get(i).text();
            text.replace("：",":");
            text.replace(" ", "");
            if (text.startsWith("又名")) {
                log.debug("又名:{}", text);
                movie.setTranslatedName(Arrays.asList(text.split(":")[1].split("/")));
            } else if (text.contains("导演")) {
                log.debug("导演:{}", text);

                if (text.split(":").length >= 2) {
                    movie.setDirector(text.split(":")[1]);
                } else {
                    movie.setDirector(text.split("：")[1]);
                }

            } else if (text.startsWith("编剧")) {
                log.debug("编剧:{}", text);
                movie.setTranslatedName(Arrays.asList(text.split(":")[1].split("/")));
            } else if (text.startsWith("主演")) {
                log.debug("主演:{}", text);
                if (text.split(":").length >= 2) {
                    actor.addAll(Arrays.asList(text.split(":")[1].split("/")));
                } else if (text.split("：").length >= 2) {
                    actor.addAll(Arrays.asList(text.split("：")[1].split("/")));
                }

            } else if (text.startsWith("类型")) {
                log.debug("类型:{}", text);
                if (text.split(":").length > 2) {
                    movie.setCategory(Arrays.asList(text.split(":")[1].split("/")));
                } else if (text.split("：").length > 2) {
                    movie.setCategory(Arrays.asList(text.split("：")[1].split("/")));
                }
            } else if (text.startsWith("制片国家/地区")) {
                log.debug("制片国家/地区:{}", text);
                movie.setOrigin(text.split(":")[1]);
            } else if (text.startsWith("语言")) {
                log.debug("语言:{}", text);
                movie.setTranslatedName(Arrays.asList(text.split(":")[1].split("/")));
            } else if (text.startsWith("上映日期")) {
                log.debug("上映日期:{}", text);
            } else if (text.startsWith("　　")) {
                log.debug("简介:{}", text);
                movie.setDescription(text);
            }else if (text.startsWith("片长")) {
                log.debug("片长", text);
                text.split(":")[1].split("/")[0].replace(" ", "");
                Pattern pat = Pattern.compile(REGEX_CHINESE);
                Matcher mat = pat.matcher(text.split(":")[1].split("/")[0].replace(" ", ""));
                movie.setDuration(Integer.parseInt(mat.replaceAll("")));
            }
        }
        List<String> downloadUrl = new ArrayList<>();
        Elements down = document.getElementsByTag("tbody");
        Document downDoc =  Jsoup.parse(down.toString());
        Elements downtr =downDoc.getElementsByClass("tab-pane1 fade in active");

        for (Element ele : downtr) {
            Elements es = ele.getAllElements().get(1).getElementsByTag("li");
            Document esDoc =  Jsoup.parse(es.toString());
            Elements a =esDoc.getElementsByTag("a");
            String str1 =  a.attr("title");
            String str2 = a.attr("href");
            downloadUrl.add(str1 + str2);
            log.debug("下载地址:{}", str1 + str2);
        }
        movie.setDownloadUrl(downloadUrl);
        return movie;
    }

}
