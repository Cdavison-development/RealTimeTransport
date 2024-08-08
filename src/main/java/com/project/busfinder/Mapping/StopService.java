package com.project.busfinder.Mapping;

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

    public Map<String, Coordinate> getStopCoordinates(List<String> stopIds) {
        Map<String, Coordinate> stopCoordinates = new HashMap<>();
        String sql = "SELECT stop_id, longitude, latitude FROM bus_stops WHERE stop_id IN (" +
                String.join(",", stopIds.stream().map(id -> "?").toArray(String[]::new)) + ")";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < stopIds.size(); i++) {
                pstmt.setString(i + 1, stopIds.get(i));
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String stopId = rs.getString("stop_id");
                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");
                stopCoordinates.put(stopId, new Coordinate(latitude, longitude));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return stopCoordinates;
    }
    }

