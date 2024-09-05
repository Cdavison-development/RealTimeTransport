package com.project.busfinder.Mapping_util;

import java.util.Arrays;
import java.util.List;

public class RouteData {
    private final String routeId;
    private final String polylineData;
    private final String stopPointRefs;

    // constructor to initialise all fields
    public RouteData(String routeId, String polylineData, String stopPointRefs) {
        this.routeId = routeId;
        this.polylineData = polylineData;
        this.stopPointRefs = stopPointRefs;
    }

    // getter for polyline data
    public String getPolylineData() {
        // System.out.println(polylineData);
        return polylineData;
    }

    // getter for stop point references as a list
    public List<String> getStopPointRefs() {
        return Arrays.asList(stopPointRefs.split(",")); // split the stop point references by commas
    }
}
