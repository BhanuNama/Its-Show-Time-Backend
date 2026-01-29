package com.excelr.repository;

import com.excelr.entity.MovieScheduleEntity;
import com.excelr.entity.VenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MovieScheduleRepository extends JpaRepository<MovieScheduleEntity, Long> {

    List<MovieScheduleEntity> findByVenueAndEndDateGreaterThanEqual(VenueEntity venue, LocalDate date);

}

