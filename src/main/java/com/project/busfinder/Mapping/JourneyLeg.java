package com.project.busfinder.Mapping;

import java.time.LocalTime;

public class JourneyLeg {
    private final String fromStop;
    private final String toStop;
    private final LocalTime departureTime;

    // constructor to initialise the from stop, to stop, and departure time
    public JourneyLeg(String fromStop, String toStop, LocalTime departureTime) {
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.departureTime = departureTime;
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

    // override toString method to provide a formatted string representation of the object
    @Override
    public String toString() {
        return String.format("From: %s, To: %s, Departure: %s", fromStop, toStop, departureTime);
    }
}
