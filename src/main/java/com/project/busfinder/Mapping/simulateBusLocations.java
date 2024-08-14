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

        initializeDatabaseConnection();
        String routeId = "345";
        String vehicleJourneyCode = "VJ_29";
        LocalTime startTime = LocalTime.of(8, 0);
        findActiveBusesInTimeFrame(startTime,0);
        findClosestDepartureTime();
        List<JourneyLeg> journeyLegs = getJourneyLegs("55", "VJ_4");


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

    //Sunday routes are problematic, their journeycodes can largely not be found in the database. I assume sunday times are different from other
    //days and therefore they use different JourneyCodes that are not specified in the XML
    public static List<JourneyInfo> findClosestDepartureTime() throws IOException, InterruptedException {
        // initialise the database connection
        initializeDatabaseConnection();

        // list to store journey information
        List<JourneyInfo> journeyInfoList = new ArrayList<>();

        // process the XML response to retrieve routes and patterns
        ArrayList<AbstractMap.SimpleEntry<String, String>> routes = readLiveLocation.processXmlResponse(fetchAndProcessResponse());

        int wrongCounter = 0;
        int rightCounter = 0;

        // iterate through each route and pattern pair
        for (AbstractMap.SimpleEntry<String, String> entry : routes) {
            String route = entry.getKey();
            String pattern = entry.getValue();

            try {
                String vehicleJourneyCode = null;

                // attempt to retrieve the vehicle journey code
                vehicleJourneyCode = getVehicleJourneyCode(route, pattern);
                rightCounter++;

                // if no vehicle journey code is found, modify the pattern and try again
                if (vehicleJourneyCode == null) {
                    wrongCounter++;
                    pattern = modifyPattern(pattern);
                    vehicleJourneyCode = getVehicleJourneyCode(route, pattern);
                }

                // if a vehicle journey code is found, query the database for journey details
                if (vehicleJourneyCode != null) {
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

                        // initialise variables to find the closest departure time
                        LocalTime currentTime = LocalTime.now();
                        LocalTime closestDepartureTime = null;
                        String closestFromStop = null;
                        String closestToStop = null;
                        double closestLongitude = 0.0;
                        double closestLatitude = 0.0;
                        long smallestDifference = Long.MAX_VALUE;

                        boolean hasDepartureTimes = false;

                        // iterate through the result set to find the closest departure time
                        while (rs2.next()) {
                            String departureTimeString = rs2.getString("departure_time");
                            String fromStop = rs2.getString("from_stop");
                            String toStop = rs2.getString("to_stop");
                            double longitude = rs2.getDouble("longitude");
                            double latitude = rs2.getDouble("latitude");

                            LocalTime depTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
                            hasDepartureTimes = true;

                            long difference = Math.abs(currentTime.toSecondOfDay() - depTime.toSecondOfDay());

                            // update the closest departure time and corresponding details
                            if (difference < smallestDifference) {
                                smallestDifference = difference;
                                closestDepartureTime = depTime;
                                closestFromStop = fromStop;
                                closestToStop = toStop;
                                closestLongitude = longitude;
                                closestLatitude = latitude;
                            }
                        }

                        // if valid departure times are found, create a JourneyInfo object and add it to the list
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // print statistics and return the list of journey information
        System.out.println("routes modified / not found: " + wrongCounter);
        System.out.println("routes found: " + (rightCounter - wrongCounter));
        System.out.println(journeyInfoList);
        return journeyInfoList;
    }

    private static String getVehicleJourneyCode(String route, String pattern) throws SQLException {
        // SQL query to fetch the Vehicle_journey_code from the journeyCode table
        String query1 = "SELECT Vehicle_journey_code FROM journeyCode WHERE route_id = ? AND journey_code = ?";

        try (PreparedStatement pstmt1 = conn.prepareStatement(query1)) {
            // set the route and pattern parameters in the SQL query
            pstmt1.setString(1, route);
            pstmt1.setString(2, pattern);

            ResultSet rs1 = pstmt1.executeQuery();

            // if a result is found, return the Vehicle_journey_code
            if (rs1.next()) {
                return rs1.getString("Vehicle_journey_code");
            }
        }

        return null; // return null if no Vehicle_journey_code is found
    }

    private static String modifyPattern(String pattern) {
        // find the index of the first '=' character in the pattern
        int index = pattern.indexOf('=');

        // if '=' is found and it is not the last character in the pattern
        if (index != -1 && index < pattern.length() - 1) {
            // extract the numeric part of the pattern after the '=' character
            String numericPart = pattern.substring(index + 1);

            // if the numeric part is longer than one character, modify it
            if (numericPart.length() > 1) {
                // replace the first digit with '1' and keep the rest unchanged
                String newNumericPart = "1" + numericPart.substring(1);
                return pattern.substring(0, index + 1) + newNumericPart; // return the modified pattern
            }
        }

        return pattern; // return the original pattern if no modification is needed
    }

    public static List<JourneyLeg> getJourneyLegs(String routeId, String vehicleJourneyCode) throws SQLException {
        List<JourneyLeg> journeyLegs = new ArrayList<>();

        // SQL query to fetch journey legs with associated bus stop coordinates
        String query = """
        SELECT jr.departure_time, jr.from_stop, jr.to_stop, bs.longitude, bs.latitude
        FROM journeyRoutes jr
        JOIN bus_stops bs ON jr.from_stop = bs.stop_id
        WHERE jr.route_id = ? AND jr.Vehicle_journey_code = ?
        ORDER BY jr.departure_time
    """;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            // set the route ID and vehicle journey code parameters in the query
            pstmt.setString(1, routeId);
            pstmt.setString(2, vehicleJourneyCode);

            ResultSet rs = pstmt.executeQuery();

            // iterate through the result set and create JourneyLeg objects
            while (rs.next()) {
                String departureTimeString = rs.getString("departure_time");
                String fromStop = rs.getString("from_stop");
                String toStop = rs.getString("to_stop");
                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");

                // parse the departure time from the string format
                LocalTime departureTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));

                // create a new JourneyLeg object and add it to the list
                JourneyLeg leg = new JourneyLeg(fromStop, toStop, departureTime);
                journeyLegs.add(leg);
            }
        }
        return journeyLegs; // return the list of journey legs
    }

    //unsure how it will handle two routes at the same time, example route: 10,Vehicle Journey Code: VJ_62, Departure Time: 08:00, From Stop: 2800S14018B, To Stop: 2800S14019A, Longitude: -2.776328, Latitude: 53.437650, route: 10,Vehicle Journey Code: VJ_63, Departure Time: 08:00, From Stop: 2800S44020B, To Stop: 2800S51011B, Longitude: -2.834953, Latitude: 53.423403,
    public static List<JourneyInfo> findActiveBusesInTimeFrame(LocalTime targetTime, int timeWindowMinutes) throws SQLException {
        initializeDatabaseConnection();

        List<JourneyInfo> journeyInfoList = new ArrayList<>();

        // SQL query to fetch active buses within a specified time frame
        String query = """
        SELECT route_id, Vehicle_journey_code, departure_time, from_stop, to_stop, bs.longitude, bs.latitude
        FROM journeyRoutes jr
        JOIN bus_stops bs ON jr.from_stop = bs.stop_id
        WHERE jr.departure_time BETWEEN ? AND ?
        ORDER BY jr.departure_time
    """;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            // calculate the start and end times based on the target time and window
            LocalTime startTime = targetTime.minusMinutes(timeWindowMinutes);
            LocalTime endTime = targetTime.plusMinutes(timeWindowMinutes);

            // set the time frame parameters in the query
            pstmt.setString(1, startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            pstmt.setString(2, endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            ResultSet rs = pstmt.executeQuery();

            // iterate through the result set and create JourneyInfo objects
            while (rs.next()) {
                String routeId = rs.getString("route_id");
                String vehicleJourneyCode = rs.getString("Vehicle_journey_code");
                String departureTimeString = rs.getString("departure_time");
                String fromStop = rs.getString("from_stop");
                String toStop = rs.getString("to_stop");
                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");

                // parse the departure time from the string format
                LocalTime departureTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));

                // create a new JourneyInfo object and add it to the list
                JourneyInfo journeyInfo = new JourneyInfo(
                        vehicleJourneyCode,
                        routeId,
                        departureTime,
                        fromStop,
                        toStop,
                        longitude,
                        latitude
                );
                journeyInfoList.add(journeyInfo);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // print the exception stack trace if an error occurs
        }

        return journeyInfoList; // return the list of active buses within the time frame
    }
}




