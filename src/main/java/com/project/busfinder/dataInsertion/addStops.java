package com.project.busfinder.dataInsertion;

import org.json.JSONObject;

import java.sql.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * how can I identify the previous stops? or stops that the bus is in between
 *
 */
public class addStops {
    private static final String DB_URL = "jdbc:sqlite:data\\databases\\routes.db";

    public static void main(String[] args) {
        String filePath = "data/stops/all_stops.json";
        try {
            String jsonData = new String(Files.readAllBytes(Paths.get(filePath)));
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                insertDataIntoDatabase(jsonData, conn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void insertDataIntoDatabase(String jsonData, Connection conn) throws Exception {
        // parse the JSON data and insert it into the bus_stops table in the database
        JSONObject obj = new JSONObject(jsonData);
        createStopsTable(conn); // ensure the bus_stops table exists

        String sql = "INSERT OR IGNORE INTO bus_stops (stop_id, common_name, longitude, latitude) VALUES (?, ?, ?, ?)";
        conn.setAutoCommit(false); // disable auto-commit for batch processing

        try {
            // iterate through each stop in the JSON data
            for (String key : obj.keySet()) {
                JSONObject stop = obj.getJSONObject(key);
                JSONObject coordinates = stop.getJSONObject("coordinates");

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    // set parameters for the SQL insert statement
                    pstmt.setString(1, key);
                    pstmt.setString(2, stop.getString("common_name"));
                    pstmt.setDouble(3, coordinates.getDouble("longitude"));
                    pstmt.setDouble(4, coordinates.getDouble("latitude"));

                    pstmt.executeUpdate(); // execute the insert
                }
            }
            conn.commit(); // commit all inserts as a single transaction
        } catch (SQLException e) {
            conn.rollback(); // rollback if an error occurs
            throw e;
        }
    }

    private static void createStopsTable(Connection conn) throws SQLException {
        // create the bus_stops table if it doesn't already exist
        String createTableSQL = "CREATE TABLE IF NOT EXISTS bus_stops (" +
                "stop_id TEXT PRIMARY KEY, " +
                "common_name TEXT, " +
                "longitude FLOAT, " +
                "latitude FLOAT)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }
}
