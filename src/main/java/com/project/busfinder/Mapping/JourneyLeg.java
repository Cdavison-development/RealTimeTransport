package com.project.busfinder.Mapping;

import java.time.LocalTime;

public class JourneyLeg {
    private final String fromStop;
    private final String toStop;
    private final LocalTime departureTime;

    public JourneyLeg(String fromStop, String toStop, LocalTime departureTime) {
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.departureTime = departureTime;
    }

    public String getFromStop() {
        return fromStop;
    }

    public String getToStop() {
        return toStop;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }
    @Override
    public String toString() {
        return String.format("From: %s, To: %s, Departure: %s", fromStop, toStop, departureTime);
    }
}
