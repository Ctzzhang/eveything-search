package com.everything.movie.repository;


import com.everything.movie.entity.Movie;
import com.everything.movie.entity.Page;
import com.everything.movie.entity.QueryDTO;

public interface IMovieRepository {

    boolean save(Movie movie, String source);

    Page<Movie> query(String queryString, int pageNo, int size);

    Page<Movie> query(QueryDTO queryDTO, int pageNo, int size);

    Movie get(String id);
}
