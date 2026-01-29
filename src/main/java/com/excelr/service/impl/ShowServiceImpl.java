package com.excelr.service.impl;

import com.excelr.entity.ShowEntity;
import com.excelr.entity.VenueEntity;
import com.excelr.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowServiceImpl {

    private final ShowRepository showRepository;

    public List<ShowEntity> getShowsForVenueAndDate(VenueEntity venue, LocalDate date) {
        return showRepository.findByVenueAndShowDate(venue, date);
    }

    public List<ShowEntity> getShowsForVenueDateAndMovie(VenueEntity venue, LocalDate date, Long tmdbMovieId) {
        return showRepository.findByVenueAndShowDateAndTmdbMovieId(venue, date, tmdbMovieId);
    }
}

