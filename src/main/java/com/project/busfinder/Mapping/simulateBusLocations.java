package com.project.busfinder.Mapping;


import com.project.busfinder.util.readLiveLocation;

import java.io.IOException;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.project.busfinder.readDatabase.getRoutes.getLiveRoutes;
import static com.project.busfinder.util.readLiveLocation.fetchAndProcessResponse;

/**
 *
 * Simulate bus locations
 *
 * Prompt API and get all current live buses
 *
 * for each live bus routes, get the expected current location by finding the closest departure time to
 * the current time.
 *
 * if buses are showing signs of being late, switch to live tracking.
 *
 *
 *
 *
 */
public class simulateBusLocations {


    private static Connection conn;

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {
        //initializeDatabaseConnection();
        /**
        try {
            List<JourneyInfo> journeyInfoList = findClosestDepartureTime();
            for (JourneyInfo info : journeyInfoList) {
                System.out.println(info);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
         **/
        initializeDatabaseConnection();
        String routeId = "345";
        String vehicleJourneyCode = "VJ_29";

        List<JourneyLeg> journeyLegs = getJourneyLegs(routeId, vehicleJourneyCode);

        // Print each JourneyLeg to verify the data
        System.out.println("Journey Legs:");
        for (JourneyLeg leg : journeyLegs) {
            System.out.println(leg);
        }
    }

    private static void initializeDatabaseConnection() {
        try {

            conn = DriverManager.getConnection("jdbc:sqlite:data/databases/routes.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<JourneyInfo> findClosestDepartureTime() throws IOException, InterruptedException {
        initializeDatabaseConnection();

        List<JourneyInfo> journeyInfoList = new ArrayList<>();


        ArrayList<AbstractMap.SimpleEntry<String, String>> routes = readLiveLocation.processXmlResponse(fetchAndProcessResponse());

        for (AbstractMap.SimpleEntry<String, String> entry : routes) {
            String route = entry.getKey();
            String pattern = entry.getValue();

            try {

                String query1 = "SELECT Vehicle_journey_code FROM journeyCode WHERE route_id = ? AND journey_code = ?";
                try (PreparedStatement pstmt1 = conn.prepareStatement(query1)) {
                    pstmt1.setString(1, route);
                    pstmt1.setString(2, pattern);

                    ResultSet rs1 = pstmt1.executeQuery();

                    while (rs1.next()) {
                        String vehicleJourneyCode = rs1.getString("Vehicle_journey_code");


                        String query2 = """
                            SELECT jr.departure_time, jr.from_stop, jr.to_stop, bs.longitude, bs.latitude
                            FROM journeyRoutes jr
                            JOIN bus_stops bs ON jr.from_stop = bs.stop_id
                            WHERE jr.route_id = ? AND jr.Vehicle_journey_code = ?
                        """;
                        try (PreparedStatement pstmt2 = conn.prepareStatement(query2)) {
                            pstmt2.setString(1, route);
                            pstmt2.setString(2, vehicleJourneyCode);

                            ResultSet rs2 = pstmt2.executeQuery();

                            LocalTime currentTime = LocalTime.now();
                            LocalTime closestDepartureTime = null;
                            String closestFromStop = null;
                            String closestToStop = null;
                            double closestLongitude = 0.0;
                            double closestLatitude = 0.0;
                            long smallestDifference = Long.MAX_VALUE;

                            boolean hasDepartureTimes = false;

                            while (rs2.next()) {

                                String departureTimeString = rs2.getString("departure_time");
                                String fromStop = rs2.getString("from_stop");
                                String toStop = rs2.getString("to_stop");
                                double longitude = rs2.getDouble("longitude");
                                double latitude = rs2.getDouble("latitude");


                                LocalTime depTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
                                hasDepartureTimes = true;

                                long difference = Math.abs(currentTime.toSecondOfDay() - depTime.toSecondOfDay());

                                if (difference < smallestDifference) {
                                    smallestDifference = difference;
                                    closestDepartureTime = depTime;
                                    closestFromStop = fromStop;
                                    closestToStop = toStop;
                                    closestLongitude = longitude;
                                    closestLatitude = latitude;
                                }
                            }

                            if (hasDepartureTimes && closestDepartureTime != null) {
                                JourneyInfo journeyInfo = new JourneyInfo(
                                        vehicleJourneyCode,
                                        route,
                                        closestDepartureTime,
                                        closestFromStop,
                                        closestToStop,
                                        closestLongitude,
                                        closestLatitude
                                );
                                journeyInfoList.add(journeyInfo);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println(journeyInfoList);
        return journeyInfoList;
    }

    public static List<JourneyLeg> getJourneyLegs(String routeId, String vehicleJourneyCode) throws SQLException {
        List<JourneyLeg> journeyLegs = new ArrayList<>();

        String query = """
            SELECT jr.departure_time, jr.from_stop, jr.to_stop, bs.longitude, bs.latitude
            FROM journeyRoutes jr
            JOIN bus_stops bs ON jr.from_stop = bs.stop_id
            WHERE jr.route_id = ? AND jr.Vehicle_journey_code = ?
            ORDER BY jr.departure_time
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, routeId);
            pstmt.setString(2, vehicleJourneyCode);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String departureTimeString = rs.getString("departure_time");
                String fromStop = rs.getString("from_stop");
                String toStop = rs.getString("to_stop");
                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");

                LocalTime departureTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));

                JourneyLeg leg = new JourneyLeg(fromStop, toStop, departureTime);
                journeyLegs.add(leg);
            }
        }
        return journeyLegs;
    }
}




