package com.project.busfinder.util;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sothawo.mapjfx.Coordinate;

import java.io.IOException;
import java.lang.reflect.Type;




public class PolylineDecoder {


    private static final double DISTANCE_THRESHOLD = 2 ;

    /**
     * When considering how we would handle data in the initial stages of data analysis in python, we had two options for storing polyline coordinate data,
     * either store a massively large Json array of coordinates for a route, Or use polyline encoding, to massively shorten the size of the string, but having
     * to decode the polyline later on. we chose the latter.
     *
     *  largely taken from https://github.com/gsanthosh91/DecodePolyline/blob/master/app/src/main/java/com/gsanthosh91/decodepolylinemap/utils/PolyUtils.java
     *
     * @param encoded
     * @return a List of coordinates for the decoded polyline
     */
    public static List<Coordinate> decodePolyline(String encoded) {
        List<Coordinate> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                if (index >= len) {
                    return polyline;
                }
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                if (index >= len) {
                    return polyline;
                }
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            Coordinate coordinate = new Coordinate(lat / 1E5, lng / 1E5);
            //System.out.println("Decoded coordinate: " + coordinate);
            polyline.add(coordinate);
        }
        return polyline;
    }

    /**
     * as the encoded polyline is a Json array, in order to store is as a basic arrayList, we append each decoded polyline
     * section to a single arrayList object. This is useful for when we want to treat a polyline as one large object
     *
     * @param json
     * @return a single list of coordinate objects
     */
    public static List<Coordinate> decodeAndConcatenatePolylinesFromString(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> encodedPolylines = gson.fromJson(json, listType);

        List<Coordinate> concatenatedPolyline = new ArrayList<>();
        for (String encodedPolyline : encodedPolylines) {
            List<Coordinate> decodedPolyline = decodePolyline(encodedPolyline);
            concatenatedPolyline.addAll(decodedPolyline);
        }

        return concatenatedPolyline;
    }
    /**
     * used to get single polyline sections, better for when we want to track individual segments.
     *
     * @param json
     * @return list of lists, where each inner list is the coordinate object for a decoded polyline
     */
    public static List<List<Coordinate>> decodePolylinesIndividually(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> encodedPolylines = gson.fromJson(json, listType);

        List<List<Coordinate>> allPolylines = new ArrayList<>();
        for (String encodedPolyline : encodedPolylines) {
            List<Coordinate> decodedPolyline = decodePolyline(encodedPolyline);
            allPolylines.add(decodedPolyline);  // add each decoded polyline as a separate list
        }

        return allPolylines;
    }


}
