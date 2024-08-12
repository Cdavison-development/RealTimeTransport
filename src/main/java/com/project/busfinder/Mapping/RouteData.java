package com.project.busfinder.Mapping;

import java.util.Arrays;
import java.util.List;

public class RouteData {
    private final String routeId;
    private final String polylineData;
    private final String stopPointRefs;

    public RouteData(String routeId, String polylineData, String stopPointRefs) {
        this.routeId = routeId;
        this.polylineData = polylineData;
        this.stopPointRefs = stopPointRefs;
    }

    public String getPolylineData() {
       // System.out.println(polylineData);
        return polylineData;
    }

    public List<String> getStopPointRefs() {
        return Arrays.asList(stopPointRefs.split(","));
    }
}
