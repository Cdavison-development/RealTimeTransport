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


    private static List<Coordinate> filterLongSegments(List<Coordinate> coordinates) {
        List<Coordinate> filteredCoordinates = new ArrayList<>();
        if (coordinates.isEmpty()) {
            return filteredCoordinates;
        }

        filteredCoordinates.add(coordinates.get(0));

        for (int i = 1; i < coordinates.size(); i++) {
            Coordinate previous = filteredCoordinates.get(filteredCoordinates.size() - 1);
            Coordinate current = coordinates.get(i);
            double distance = haversineDistance(previous.getLatitude(), previous.getLongitude(), current.getLatitude(), current.getLongitude());
            System.out.printf("Distance from [%f, %f] to [%f, %f]: %f km%n",
                    previous.getLatitude(), previous.getLongitude(),
                    current.getLatitude(), current.getLongitude(),
                    distance);
            if (distance <= DISTANCE_THRESHOLD) {
                filteredCoordinates.add(current);
            } else {
                System.out.println("Skipping segment from " + previous + " to " + current + " due to long distance: " + distance + " km");
            }
        }

        return filteredCoordinates;
    }

    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // radius of earth


        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);


        System.out.printf("Lat1: %f, Lon1: %f, Lat2: %f, Lon2: %f%n", lat1, lon1, lat2, lon2);
        System.out.printf("Lat1 (rad): %f, Lon1 (rad): %f, Lat2 (rad): %f, Lon2 (rad): %f%n", lat1Rad, lon1Rad, lat2Rad, lon2Rad);


        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;


        System.out.printf("dLat: %f, dLon: %f%n", dLat, dLon);


        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;


        System.out.printf("a: %f, c: %f, Distance: %f km%n", a, c, distance);

        return distance;
    }
    public static void main(String[] args) {

        String jsonString = "[\"wbgeIjytOPiCQE@LCVW|CILC?MCM|AARYvCq@`FD?RN@XLdA^tERhCUNy@T\","
                + "\"{fgeIjzuOx@Un@a@dAg@d@g@`AgAL@fCpEr@nA\","
                + "\"qxfeI`{uOGLdAhB^JBGNGHBHJBP?RENCDJdBh@bBHPCJOZi@bAWf@o@nB]z@MTsAdBQVM^?PC`@ENJNJr@Nl@b@nARl@@C\","
                + "\"_yfeI`awOABVt@ZhAjBtFZFTPlAp@fBzA`BpBbBlCf@lA\","
                + "\"kefeIp|wOPl@h@dCtA`IDd@Ab@Ip@_@zBFD\"]";

        //List<Coordinate> polyline = decodeAndConcatenatePolylinesFromString(routeData.getPolylineData());
        //System.out.println("Concatenated Polyline:");
        //for (Coordinate coord : polyline) {
            //System.out.println("Lat: " + coord.getLatitude() + ", Lon: " + coord.getLongitude());
        //}
    }

    public static void writeLinestringToFile(String linestring, String filePath) throws IOException {
        // Write the LINESTRING to a file using a BufferedWriter
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(linestring);
        }
    }
}
