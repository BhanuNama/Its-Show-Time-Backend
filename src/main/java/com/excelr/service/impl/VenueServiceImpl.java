package com.excelr.service.impl;

import com.excelr.entity.UserEntity;
import com.excelr.entity.VenueEntity;
import com.excelr.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl {

    private final VenueRepository venueRepository;

    public VenueEntity createVenue(VenueEntity venue, UserEntity owner) {
        venue.setOwner(owner);
        return venueRepository.save(venue);
    }

    public List<VenueEntity> getVenuesByOwner(UserEntity owner) {
        return venueRepository.findByOwner(owner);
    }
}

