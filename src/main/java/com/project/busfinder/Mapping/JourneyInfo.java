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

    // constructor to initialise all fields
    public JourneyInfo(String vehicleJourneyCode, String route, LocalTime closestDepartureTime, String fromStop, String toStop, double longitude, double latitude) {
        this.vehicleJourneyCode = vehicleJourneyCode;
        this.route = route;
        this.closestDepartureTime = closestDepartureTime;
        this.fromStop = fromStop;
        this.toStop = toStop;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    // getter for vehicle journey code
    public String getVehicleJourneyCode() {
        return vehicleJourneyCode;
    }

    // getter for route
    public String getRoute() {
        return route;
    }

    // getter for closest departure time
    public LocalTime getClosestDepartureTime() {
        return closestDepartureTime;
    }

    // getter for from stop
    public String getFromStop() {
        return fromStop;
    }

    // getter for to stop
    public String getToStop() {
        return toStop;
    }

    // getter for longitude
    public double getLongitude() {
        return longitude;
    }

    // getter for latitude
    public double getLatitude() {
        return latitude;
    }

    // override toString method to provide a formatted string representation of the object
    @Override
    public String toString() {
        return String.format("route: %s, Vehicle Journey Code: %s, Departure Time: %s, From Stop: %s, To Stop: %s, Longitude: %.6f, Latitude: %.6f",
                route, vehicleJourneyCode, closestDepartureTime, fromStop, toStop, longitude, latitude);
    }


}