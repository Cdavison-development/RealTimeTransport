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
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public RouteData getRouteData(String routeId) {
        String sql = "SELECT polyline_data, stop_point_refs FROM routes WHERE route_id = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, routeId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String polylineData = rs.getString("polyline_data");
                String stopPointRefs = rs.getString("stop_point_refs");
                return new RouteData(routeId, polylineData, stopPointRefs);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }


}
