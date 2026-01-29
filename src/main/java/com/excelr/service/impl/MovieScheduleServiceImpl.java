package com.excelr.service.impl;

import com.excelr.entity.MovieScheduleEntity;
import com.excelr.entity.ShowEntity;
import com.excelr.entity.Status;
import com.excelr.entity.VenueEntity;
import com.excelr.repository.MovieScheduleRepository;
import com.excelr.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieScheduleServiceImpl {

    private final MovieScheduleRepository movieScheduleRepository;
    private final ShowRepository showRepository;

    public MovieScheduleEntity createSchedule(MovieScheduleEntity schedule) {
        return movieScheduleRepository.save(schedule);
    }

    public List<MovieScheduleEntity> getActiveSchedulesForVenueFromDate(VenueEntity venue, LocalDate fromDate) {
        return movieScheduleRepository.findByVenueAndEndDateGreaterThanEqual(venue, fromDate);
    }

    public List<ShowEntity> generateShowsForSchedule(MovieScheduleEntity schedule) {
        // Very simple implementation: parse showtimes JSON as a comma-separated list
        // In production, you would use a proper JSON library.
        List<String> showtimes = parseShowtimes(schedule.getShowtimes());

        List<ShowEntity> showsToSave = new ArrayList<>();
        LocalDate current = schedule.getStartDate();
        while (!current.isAfter(schedule.getEndDate())) {
            for (String time : showtimes) {
                ShowEntity show = ShowEntity.builder()
                        .venue(schedule.getVenue())
                        .schedule(schedule)
                        .tmdbMovieId(schedule.getTmdbMovieId())
                        .showDate(current)
                        .showTime(time)
                        .silverPrice(schedule.getSilverPrice() != null
                                ? schedule.getSilverPrice()
                                : BigDecimal.ZERO)
                        .goldPrice(schedule.getGoldPrice() != null
                                ? schedule.getGoldPrice()
                                : BigDecimal.ZERO)
                        .vipPrice(schedule.getVipPrice() != null
                                ? schedule.getVipPrice()
                                : BigDecimal.ZERO)
                        .status(Status.ACTIVE)
                        .build();
                showsToSave.add(show);
            }
            current = current.plusDays(1);
        }
        return showRepository.saveAll(showsToSave);
    }

    private List<String> parseShowtimes(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        // Expecting simple JSON-like array: ["09:00 AM","12:00 PM"]
        String cleaned = raw.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        for (String part : cleaned.split(",")) {
            String time = part.trim();
            if (time.startsWith("\"") && time.endsWith("\"") && time.length() >= 2) {
                time = time.substring(1, time.length() - 1);
            }
            if (!time.isEmpty()) {
                result.add(time);
            }
        }
        return result;
    }
}

