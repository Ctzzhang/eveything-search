package com.everything.web;

import com.everything.movie.spider.dytiantang.MovieListParser;
import com.everything.movie.spider.mp4Ba.Mp4ListParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@Slf4j
@RequestMapping("/spider/movie")
public class ManagerController {

    private static final ReentrantLock lock = new ReentrantLock();

    @Autowired
    private MovieListParser movieListParser;

    @Autowired
    private Mp4ListParser mp4ListParser;

    @GetMapping("/dytiantang")
    public String dytiantang(@RequestParam(value = "p",required = false,
            defaultValue = MovieListParser.START_PAGE+MovieListParser.JINGDIAN_PAGE + MovieListParser.HTML) String page) {
        try {
            if (lock.tryLock()) {
                movieListParser.parse(page);
                return "爬虫执行完成";
            } else {
                return "请勿重复执行";
            }
        } catch (IOException e) {
            log.error("爬虫发生异常", e);
            return e.getMessage();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @GetMapping("/mp4ba")
    public String mp4ba (@RequestParam(value = "p", required = false,
            defaultValue = Mp4ListParser.ALL_PAGE + Mp4ListParser.START_PAGE) String page) {
        try {
            if (lock.tryLock()) {
                mp4ListParser.parse(page);
                return "爬虫执行完成";
            } else {
                return "请勿重复执行";
            }
        } catch (IOException e) {
            log.error("爬虫发生异常", e);
            return e.getMessage();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
