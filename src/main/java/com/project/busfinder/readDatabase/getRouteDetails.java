package com.project.busfinder.readDatabase;

import com.project.busfinder.GUI.TrackBusPanelController;

import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class getRouteDetails {

    private final Connection conn;

    public getRouteDetails(Connection conn) {
        this.conn = conn;
    }

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:data/databases/routes.db")) {
            getRouteDetails fetcher = new getRouteDetails(conn);
            List<getRouteDetails.JourneyRouteInfo> journeyInfoList = fetcher.getJourneyRouteInfo("10A");

            for (getRouteDetails.JourneyRouteInfo info : journeyInfoList) {
                System.out.println(info);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<JourneyRouteInfo> getJourneyRouteInfo(String routeId) throws SQLException {
        // SQL query to retrieve journey route information, including first and last stops and their times
        String query = """
    WITH FirstStop AS (
        SELECT 
            journey_pattern_ref,
            vehicle_journey_code,
            from_stop AS first_from_stop,
            MIN(departure_time) AS earliest_departure_time
        FROM 
            journeyRoutes
        WHERE 
            route_id = ?
        GROUP BY 
            journey_pattern_ref,
            vehicle_journey_code
    ),
    LastStop AS (
        SELECT 
            journey_pattern_ref,
            vehicle_journey_code,
            to_stop AS last_to_stop,
            MAX(departure_time) AS latest_departure_time
        FROM 
            journeyRoutes
        WHERE 
            route_id = ?
        GROUP BY 
            journey_pattern_ref,
            vehicle_journey_code
    )
    SELECT
        fs.journey_pattern_ref,
        fs.vehicle_journey_code,
        fs.first_from_stop,
        bs1.common_name AS first_from_stop_name,
        ls.last_to_stop,
        bs2.common_name AS last_to_stop_name,
        fs.earliest_departure_time,
        ls.latest_departure_time
    FROM 
        FirstStop fs
    JOIN 
        LastStop ls ON fs.journey_pattern_ref = ls.journey_pattern_ref 
                    AND fs.vehicle_journey_code = ls.vehicle_journey_code
    JOIN 
        bus_stops bs1 ON fs.first_from_stop = bs1.stop_id
    JOIN 
        bus_stops bs2 ON ls.last_to_stop = bs2.stop_id;
    """;

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            // set the route ID parameter for both instances in the query
            pstmt.setString(1, routeId);
            pstmt.setString(2, routeId);
            ResultSet rs = pstmt.executeQuery();

            List<JourneyRouteInfo> results = new ArrayList<>();

            // process the result set and create JourneyRouteInfo objects
            while (rs.next()) {
                String journeyPatternRef = rs.getString("journey_pattern_ref");
                String vehicleJourneyCode = rs.getString("vehicle_journey_code");
                String firstFromStop = rs.getString("first_from_stop");
                String firstFromStopName = rs.getString("first_from_stop_name");
                String lastToStop = rs.getString("last_to_stop");
                String lastToStopName = rs.getString("last_to_stop_name");

                // retrieve and parse the earliest departure time and latest arrival time
                String departureTimeString = rs.getString("earliest_departure_time");
                String arrivalTimeString = rs.getString("latest_departure_time");
                LocalTime earliestDepartureTime = LocalTime.parse(departureTimeString);
                LocalTime latestArrivalTime = LocalTime.parse(arrivalTimeString);

                // check if the route is circular (first and last stop names are the same)
                boolean isCircular = firstFromStopName.equals(lastToStopName);
                System.out.println(firstFromStopName + " " + lastToStopName);

                // create a new JourneyRouteInfo object and add it to the results list
                JourneyRouteInfo info = new JourneyRouteInfo(
                        journeyPatternRef,
                        vehicleJourneyCode,
                        firstFromStop,
                        firstFromStopName,
                        lastToStop,
                        lastToStopName,
                        earliestDepartureTime,
                        latestArrivalTime,
                        isCircular
                );
                results.add(info);
            }

            // sort the results by the earliest departure time
            results.sort(Comparator.comparing(JourneyRouteInfo::getEarliestDepartureTime));

            return results; // return the sorted list of JourneyRouteInfo objects
        }
    }

    public static class JourneyRouteInfo {
        private final String journeyPatternRef;
        private final String vehicleJourneyCode;
        private final String firstFromStop;
        private final String firstFromStopName;
        private final String lastToStop;
        private final String lastToStopName;
        private final LocalTime earliestDepartureTime;
        private final LocalTime latestArrivalTime;
        private final boolean isCircular;

        // constructor to initialise all fields
        public JourneyRouteInfo(String journeyPatternRef, String vehicleJourneyCode, String firstFromStop,
                                String firstFromStopName, String lastToStop, String lastToStopName,
                                LocalTime earliestDepartureTime, LocalTime latestArrivalTime, boolean isCircular) {
            this.journeyPatternRef = journeyPatternRef;
            this.vehicleJourneyCode = vehicleJourneyCode;
            this.firstFromStop = firstFromStop;
            this.firstFromStopName = firstFromStopName;
            this.lastToStop = lastToStop;
            this.lastToStopName = lastToStopName;
            this.earliestDepartureTime = earliestDepartureTime;
            this.latestArrivalTime = latestArrivalTime;
            this.isCircular = isCircular;
        }

        // getter for journey pattern reference
        public String getJourneyPatternRef() {
            return journeyPatternRef;
        }

        // getter for vehicle journey code
        public String getVehicleJourneyCode() {
            return vehicleJourneyCode;
        }

        // getter for the first 'from' stop ID
        public String getFirstFromStop() {
            return firstFromStop;
        }

        // getter for the first 'from' stop name
        public String getFirstFromStopName() {
            return firstFromStopName;
        }

        // getter for the last 'to' stop ID
        public String getLastToStop() {
            return lastToStop;
        }

        // getter for the last 'to' stop name
        public String getLastToStopName() {
            return lastToStopName;
        }

        // getter for the earliest departure time
        public LocalTime getEarliestDepartureTime() {
            return earliestDepartureTime;
        }

        // getter for the latest arrival time
        public LocalTime getLatestArrivalTime() {
            return latestArrivalTime;
        }

        // method to check if the route is circular
        public boolean isCircular() {
            return isCircular;
        }

        // override toString method to provide a formatted string representation of the object
        @Override
        public String toString() {
            String routeDescription = isCircular ? "Circular Route: " : "Route: ";
            return String.format("%sJourneyPatternRef: %s, VehicleJourneyCode: %s, %s (%s) -> %s (%s), Departure: %s, Arrival: %s",
                    routeDescription, journeyPatternRef, vehicleJourneyCode,
                    firstFromStopName, firstFromStop, lastToStopName, lastToStop,
                    earliestDepartureTime, latestArrivalTime);
        }
    }
}