package com.project.busfinder.Mapping_util;

import com.sothawo.mapjfx.Coordinate;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StopService {

        private Connection connect() {
            // SQLite connection string
            String url = "jdbc:sqlite:data/databases/routes.db"; // Adjust the path accordingly
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(url);
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            return conn;
        }

    /**
     *
     * gets coordinates for a list of bus stops by querying the database to find the coordinates for each stop ID,
     *
     * @param stopIds
     * @return a map where each key is a stop ID and its value is the corresponding coordinate object
     */
    public Map<String, Coordinate> getStopCoordinates(List<String> stopIds) {
        // map to store stop IDs and their corresponding coordinates
        Map<String, Coordinate> stopCoordinates = new HashMap<>();

        // build the SQL query with placeholders for the stop IDs
        String sql = "SELECT stop_id, longitude, latitude FROM bus_stops WHERE stop_id IN (" +
                String.join(",", stopIds.stream().map(id -> "?").toArray(String[]::new)) + ")";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the stop ID parameters in the SQL query
            for (int i = 0; i < stopIds.size(); i++) {
                pstmt.setString(i + 1, stopIds.get(i));
            }

            // execute the query and retrieve the result set
            ResultSet rs = pstmt.executeQuery();

            // iterate through the result set and populate the map with stop IDs and coordinates
            while (rs.next()) {
                String stopId = rs.getString("stop_id");
                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");
                stopCoordinates.put(stopId, new Coordinate(latitude, longitude));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage()); // print any SQL exception messages
        }

        return stopCoordinates; // return the map of stop coordinates
    }

    /**
     *
     * queries database to get long and lat values for a given stop ID
     *
     * @param stopId
     * @return coordinate object storing long and lat values
     * @throws SQLException
     */
    public Coordinate getCoordinates(String stopId) throws SQLException {
        String query = "SELECT longitude, latitude FROM bus_stops WHERE stop_id = ?";

        try (Connection conn = this.connect();
        PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stopId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");
                return new Coordinate(latitude, longitude);
            } else {
                throw new SQLException("Coordinates not found for stop ID: " + stopId);
            }
        }
    }
}

