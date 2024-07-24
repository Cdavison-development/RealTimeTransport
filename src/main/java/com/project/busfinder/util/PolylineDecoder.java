package com.project.busfinder.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;


public class PolylineDecoder {
    //public field for latitude and longitude
    public static class Coordinate {
        public double latitude;
        public double longitude;
        // initialise latitude and longitude
        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public String toString() {
            // Coordinate in "longitude latitude" format for LINESTRING compatibility
            return longitude + " " + latitude;
        }
    }
    public static List<Coordinate> decodePolyline(String encoded) {
        //list to store decoded coordinates
        List<Coordinate> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        // Loop through the encoded polyline string
        while (index < len) {
            int b, shift = 0, result = 0;
            // decode latitude
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;

            //decode longitude
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            //convert to decimal degrees and add to the list
            double latD = lat / 1E5;
            double lngD = lng / 1E5;
            polyline.add(new Coordinate(latD, lngD));
        }
        //return list of decoded coordinates
        return polyline;
    }

    public static String readPolylineFromFile(String filePath) throws IOException {
        //read encoded polyline from file
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static void main(String[] args) {
        try {
            // read encoded polyline from file
            String polyline = readPolylineFromFile("data/encoded_polyline.txt");
            // Decode the polyline into a list of coordinates
            List<Coordinate> decoded = decodePolyline(polyline);

            // Generate LINESTRING format
            String linestring = "LINESTRING (" + decoded.stream()
                    .map(Coordinate::toString)
                    .collect(Collectors.joining(", ")) + ")";

            // Write Linestring to file
            writeLinestringToFile(linestring, "data/linestring_output.txt");

            System.out.println("Linestring has been written to file.");
            System.out.println("Total Coordinates: " + decoded.size());
        } catch (IOException e) {
            //handle exceptions
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void writeLinestringToFile(String linestring, String filePath) throws IOException {
        // Write the LINESTRING to a file using a BufferedWriter
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(linestring);
        }
    }
}
