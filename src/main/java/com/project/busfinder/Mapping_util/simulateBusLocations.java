package com.project.busfinder.Mapping_util;


import java.io.IOException;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
 * if the slected day allows live tracking, switch to live tracking on day selection
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

    }

    public static void initializeDatabaseConnection() {
        try {

            conn = DriverManager.getConnection("jdbc:sqlite:data/databases/routes.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static vjcAndDay getVehicleJourneyCode(String route, String pattern) throws SQLException {
        initializeDatabaseConnection();
        // SQL query to fetch the Vehicle_journey_code from the journeyCode table
        String query1 = "SELECT Vehicle_journey_code, days_of_week FROM journeyCode WHERE route_id = ? AND journey_code = ?";

        try (PreparedStatement pstmt1 = conn.prepareStatement(query1)) {
            // set the route and pattern parameters in the SQL query
            pstmt1.setString(1, route);
            pstmt1.setString(2, pattern);

            ResultSet rs1 = pstmt1.executeQuery();

            // if a result is found, return the Vehicle_journey_code
            if (rs1.next()) {
                String vehicleJourneyCode = rs1.getString("Vehicle_journey_code");
                String day = rs1.getString("days_of_week");
                return new vjcAndDay(vehicleJourneyCode, day);
            }
        }

        return null; // return null if no Vehicle_journey_code is found
    }

    public static List<JourneyLeg> getJourneyLegs(String routeId, String vehicleJourneyCode, String day) throws SQLException {
        List<JourneyLeg> journeyLegs = new ArrayList<>();
        String tableName = switch (day) {
            case "Saturday" -> "saturday_routes";
            case "Sunday" -> "sunday_routes";
            case "Monday" -> "monday_routes";
            case "Tuesday" -> "tuesday_routes";
            case "Wednesday" -> "wednesday_routes";
            case "Thursday" -> "thursday_routes";
            case "Friday" -> "friday_routes";
            default -> throw new IllegalArgumentException("Invalid day of the week: " + day);
        };

        // SQL query to fetch journey legs with associated bus stop coordinates
        String query = String.format("""
    SELECT jr.departure_time, jr.date, jr.from_stop, jr.to_stop, bs.longitude, bs.latitude
    FROM %s jr
    JOIN bus_stops bs ON jr.from_stop = bs.stop_id
    WHERE jr.route_id = ? AND jr.Vehicle_journey_code = ?
    ORDER BY jr.date ASC, jr.departure_time ASC
    """, tableName);

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            // set the route ID and vehicle journey code parameters in the query
            pstmt.setString(1, routeId);
            pstmt.setString(2, vehicleJourneyCode);

            ResultSet rs = pstmt.executeQuery();

            // iterate through the result set and create JourneyLeg objects
            while (rs.next()) {
                String departureTimeString = rs.getString("departure_time");
                long dateMillis = rs.getLong("date");
                LocalDate departureDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate();
                String fromStop = rs.getString("from_stop");
                String toStop = rs.getString("to_stop");
                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");

                // parse the departure time from the string format
                LocalTime departureTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));

                //check if the route crosses into a new day
                if (departureTime.equals(LocalTime.MIDNIGHT)) {
                    departureDate = departureDate.plusDays(1);
                }

                // create a new JourneyLeg object and add it to the list
                JourneyLeg leg = new JourneyLeg(fromStop, toStop, departureTime, departureDate,longitude,latitude);
                journeyLegs.add(leg);
            }
        }
        return journeyLegs;
    }

    //unsure how it will handle two routes at the same time, example route: 10,Vehicle Journey Code: VJ_62, Departure Time: 08:00, From Stop: 2800S14018B, To Stop: 2800S14019A, Longitude: -2.776328, Latitude: 53.437650, route: 10,Vehicle Journey Code: VJ_63, Departure Time: 08:00, From Stop: 2800S44020B, To Stop: 2800S51011B, Longitude: -2.834953, Latitude: 53.423403,
    public static List<JourneyInfo> findActiveBusesInTimeFrame(LocalTime targetTime, int timeWindowMinutes, String day) throws SQLException {
        initializeDatabaseConnection();

        List<JourneyInfo> journeyInfoList = new ArrayList<>();

        String tableName = switch (day) {
            case "Saturday" -> "saturday_routes";
            case "Sunday" -> "sunday_routes";
            case "Monday" -> "monday_routes";
            case "Tuesday" -> "tuesday_routes";
            case "Wednesday" -> "wednesday_routes";
            case "Thursday" -> "thursday_routes";
            case "Friday" -> "friday_routes";
            default -> throw new IllegalArgumentException("Invalid day of the week: " + day);
        };

        // SQL query to fetch active buses within a specified time frame
        String query = String.format("""
        SELECT route_id, Vehicle_journey_code, departure_time, from_stop, to_stop, MAX(jr.departure_time) AS end_time, bs.longitude, bs.latitude
        FROM %s jr
        JOIN bus_stops bs ON jr.from_stop = bs.stop_id
        GROUP BY route_id, Vehicle_journey_code, jr.from_stop, bs.longitude, bs.latitude
        HAVING (MIN(jr.departure_time) BETWEEN ? AND ?)
        OR (MIN(jr.departure_time) >= ? AND MIN(jr.departure_time) < '00:00:00')
        ORDER BY MIN(jr.departure_time)
    """, tableName);

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            // calculate the start and end times based on the target time and window
            LocalTime startTime = targetTime.minusMinutes(timeWindowMinutes);
            LocalTime endTime = targetTime.plusMinutes(timeWindowMinutes);

            // set the time frame parameters in the query
            pstmt.setString(1, startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            pstmt.setString(2, endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            if (endTime.isBefore(startTime)) {
                pstmt.setString(3, startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else {
                pstmt.setString(3, "00:00:00");
            }

            ResultSet rs = pstmt.executeQuery();

            // process each result and create journeyinfo objects
            while (rs.next()) {
                String routeId = rs.getString("route_id");
                String vehicleJourneyCode = rs.getString("Vehicle_journey_code");
                String departureTimeString = rs.getString("departure_time");
                String toStop = rs.getString("to_stop");
                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");

                // parse departure time
                LocalTime departureTime = LocalTime.parse(departureTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
                String endTimeString = rs.getString("end_time");
                LocalTime endTimeForJourney = LocalTime.parse(endTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));

                // check if the bus ends after the target time
                if (endTimeForJourney.isAfter(targetTime)) {
                    // create and add a new journeyinfo object to the list
                    JourneyInfo journeyInfo = new JourneyInfo(
                            vehicleJourneyCode,
                            routeId,
                            departureTime,
                            rs.getString("from_stop"),
                            toStop,
                            longitude,
                            latitude
                    );
                    journeyInfoList.add(journeyInfo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return journeyInfoList;
    }
}




