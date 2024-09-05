package com.project.busfinder.Mapping_util;

public class LiveRouteInfo {
    private String lineRef;
    private String journeyRef;
    private double latitude;
    private double longitude;

    public LiveRouteInfo(String lineRef, String journeyRef, double latitude, double longitude) {
        this.lineRef = lineRef;
        this.journeyRef = journeyRef;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLineRef() {
        return lineRef;
    }

    public String getJourneyRef() {
        return journeyRef;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "RouteInfo{" +
                "lineRef='" + lineRef + '\'' +
                ", journeyRef='" + journeyRef + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}