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

import static com.everything.movie.spider.dytiantang.MovieListParser.userAgentList;
import static java.util.stream.Collectors.toList;

@Component
@Slf4j
public class Test {


    public static void main(String[] args) throws IOException {
        Mp4ListParser.parse(Mp4ListParser.START_PAGE);
    }
}
