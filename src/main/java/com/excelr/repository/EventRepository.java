package com.excelr.repository;

import com.excelr.entity.EventEntity;
import com.excelr.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    @EntityGraph(attributePaths = {"venue"})
    List<EventEntity> findByOwner(UserEntity owner);

    @EntityGraph(attributePaths = {"venue"})
    @Override
    List<EventEntity> findAll();

    @EntityGraph(attributePaths = {"venue"})
    @Override
    Optional<EventEntity> findById(Long id);

}
