package com.excelr.repository;

import com.excelr.entity.ShowEntity;
import com.excelr.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ShowRepository extends JpaRepository<ShowEntity, Long> {

    List<ShowEntity> findByVenueAndShowDate(VenueEntity venue, LocalDate showDate);

    List<ShowEntity> findByVenueAndShowDateAndTmdbMovieId(VenueEntity venue, LocalDate showDate, Long tmdbMovieId);

}

