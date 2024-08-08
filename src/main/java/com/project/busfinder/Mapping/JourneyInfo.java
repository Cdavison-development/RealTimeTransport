package com.project.busfinder.Mapping;

import java.time.LocalTime;
import java.util.List;

public class JourneyInfo {
    private final String vehicleJourneyCode;
    private final String route;
    private final LocalTime closestDepartureTime;
    private final String fromStop;
    private final String toStop;
    private final double longitude;
    private final double latitude;

    public JourneyInfo(String vehicleJourneyCode, String route, LocalTime closestDepartureTime, String fromStop, String toStop, double longitude, double latitude) {
        this.vehicleJourneyCode = vehicleJourneyCode;
        this.route = route;
        this.closestDepartureTime = closestDepartureTime;
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getVehicleJourneyCode() {
        return vehicleJourneyCode;
    }
    public String getRoute() {
        return route;
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
        return String.format("route: %s,Vehicle Journey Code: %s, Departure Time: %s, From Stop: %s, To Stop: %s, Longitude: %.6f, Latitude: %.6f",route,
                vehicleJourneyCode, closestDepartureTime, fromStop, toStop, longitude, latitude);
    }


}