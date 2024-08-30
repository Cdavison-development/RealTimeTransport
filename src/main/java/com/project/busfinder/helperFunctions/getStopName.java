package com.project.busfinder.helperFunctions;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.project.busfinder.helperFunctions.makeConnection.conn;

public class getStopName {
    static {
        try {
            // initialise the connection here
            conn = DriverManager.getConnection("jdbc:sqlite:data/databases/routes.db");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError("Failed to initialize database connection.");
        }
    }
    public static String StopName(String stopId) throws SQLException {
        String query = "SELECT common_name FROM bus_stops WHERE stop_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, stopId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("common_name");
            }
        }
        return "Unknown Stop";
    }
}
