package com.excelr.repository;

import com.excelr.entity.VenueEntity;
import com.excelr.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueRepository extends JpaRepository<VenueEntity, Long> {

    List<VenueEntity> findByOwner(UserEntity owner);

}

