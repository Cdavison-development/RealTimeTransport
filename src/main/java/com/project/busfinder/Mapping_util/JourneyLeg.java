package com.project.busfinder.Mapping_util;

import java.time.LocalDate;
import java.time.LocalTime;

public class JourneyLeg {
    private final String fromStop;
    private final String toStop;
    private final LocalTime departureTime;
    private final LocalDate departureDate;
    private final Double longitude;
    private final Double latitude;
    // constructor to initialise the from stop, to stop, and departure time
    public JourneyLeg(String fromStop, String toStop, LocalTime departureTime, LocalDate departureDate,double longitude,double latitude) {
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.departureTime = departureTime;
        this.departureDate = departureDate;
        this.longitude = longitude;
        this.latitude = latitude;

    }

    // getter for the from stop
    public String getFromStop() {
        return fromStop;
    }

    // getter for the to stop
    public String getToStop() {
        return toStop;
    }

    // getter for the departure time
    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }
    public Double getLongitude() {
        return longitude;
    }
    public Double getLatitude() {
        return latitude;
    }
    // override toString method to provide a formatted string representation of the object
    @Override
    public String toString() {
        return String.format("From: %s, To: %s, Departure: %s", fromStop, toStop, departureTime);
    }
}
