package com.project.busfinder.Mapping;

import java.time.LocalTime;

public class JourneyInfo {
    private final String vehicleJourneyCode;
    private final LocalTime closestDepartureTime;
    private final String fromStop;
    private final String toStop;
    private final double longitude;
    private final double latitude;

    public JourneyInfo(String vehicleJourneyCode, LocalTime closestDepartureTime, String fromStop, String toStop, double longitude, double latitude) {
        this.vehicleJourneyCode = vehicleJourneyCode;
        this.closestDepartureTime = closestDepartureTime;
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getVehicleJourneyCode() {
        return vehicleJourneyCode;
    }

    public LocalTime getClosestDepartureTime() {
        return closestDepartureTime;
    }

    public String getFromStop() {
        return fromStop;
    }

    public String getToStop() {
        return toStop;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    @Override
    public String toString() {
        return String.format("Vehicle Journey Code: %s, Departure Time: %s, From Stop: %s, To Stop: %s, Longitude: %.6f, Latitude: %.6f",
                vehicleJourneyCode, closestDepartureTime, fromStop, toStop, longitude, latitude);
    }
}