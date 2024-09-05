package com.project.busfinder.Mapping_util;

public class vjcAndDay {

    private String vehicleJourneyCode;
    private String day;

    public vjcAndDay(String vehicleJourneyCode, String day) {
        this.vehicleJourneyCode = vehicleJourneyCode;
        this.day = day;
    }

    public String getVehicleJourneyCode() {
        return vehicleJourneyCode;
    }

    public String getDay() {
        return day;
    }

    @Override
    public String toString() {
        return "JourneyInfo{vehicleJourneyCode='" + vehicleJourneyCode + "', day='" + day + "'}";
    }
    }

