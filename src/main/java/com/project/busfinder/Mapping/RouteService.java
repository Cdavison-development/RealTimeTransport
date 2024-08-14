package com.project.busfinder.Mapping;

import java.sql.*;
import java.time.LocalTime;
import java.util.Optional;

public class RouteService {

    private Connection connect() {
        // SQLite connection string
        String url = "jdbc:sqlite:data\\databases\\routes.db";
        Connection conn = null;
        try {
            // establish a connection to the SQLite database
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage()); // print any SQL exception messages
        }
        return conn; // return the established connection, or null if the connection failed
    }

    public RouteData getRouteData(String routeId) {
        String sql = "SELECT polyline_data, stop_point_refs FROM routes WHERE route_id = ?";
        try (Connection conn = this.connect(); // establish a connection to the database
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // set the route ID parameter in the SQL query
            pstmt.setString(1, routeId);
            ResultSet rs = pstmt.executeQuery();

            // if a result is found, retrieve the polyline data and stop point references
            if (rs.next()) {
                String polylineData = rs.getString("polyline_data");
                String stopPointRefs = rs.getString("stop_point_refs");
                return new RouteData(routeId, polylineData, stopPointRefs); // return a RouteData object
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage()); // print any SQL exception messages
        }
        return null; // return null if no data is found or an exception occurs
    }


}
