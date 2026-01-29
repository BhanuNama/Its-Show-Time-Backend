package com.excelr.service.impl;

import com.excelr.entity.EventEntity;
import com.excelr.entity.UserEntity;
import com.excelr.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl {

    private final EventRepository eventRepository;

    public EventEntity createEvent(EventEntity event, UserEntity owner) {
        event.setOwner(owner);
        return eventRepository.save(event);
    }

    public List<EventEntity> getEventsByOwner(UserEntity owner) {
        return eventRepository.findByOwner(owner);
    }
}

