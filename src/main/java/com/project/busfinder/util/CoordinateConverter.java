package com.project.busfinder.util;

import com.sothawo.mapjfx.Coordinate;
import java.util.ArrayList;
import java.util.List;

public class CoordinateConverter {

    public static List<Coordinate> convertToMapjfxCoordinates(List<com.project.busfinder.util.Coordinate> customCoordinates) {
        List<Coordinate> mapjfxCoordinates = new ArrayList<>();

        for (com.project.busfinder.util.Coordinate customCoord : customCoordinates) {
            double latitude = customCoord.getLatitude();
            double longitude = customCoord.getLongitude();
            mapjfxCoordinates.add(new Coordinate(latitude, longitude));
        }

        return mapjfxCoordinates;
    }
}
