package com.excelr.entity;

/**
 * Shared lifecycle status for schedules, shows, events and bookings.
 */
public enum Status {
    ACTIVE,      // for schedules/shows/events that are currently valid
    CANCELLED,
    COMPLETED,
    CONFIRMED    // typically for bookings when ticket is issued
}

