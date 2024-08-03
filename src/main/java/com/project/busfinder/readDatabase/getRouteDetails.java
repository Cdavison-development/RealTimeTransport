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
            pstmt.setString(1, routeId);
            pstmt.setString(2, routeId);
            ResultSet rs = pstmt.executeQuery();

            List<JourneyRouteInfo> results = new ArrayList<>();

            while (rs.next()) {
                String journeyPatternRef = rs.getString("journey_pattern_ref");
                String vehicleJourneyCode = rs.getString("vehicle_journey_code");
                String firstFromStop = rs.getString("first_from_stop");
                String firstFromStopName = rs.getString("first_from_stop_name");
                String lastToStop = rs.getString("last_to_stop");
                String lastToStopName = rs.getString("last_to_stop_name");

                // retrieve the times as strings and parse them
                String departureTimeString = rs.getString("earliest_departure_time");
                String arrivalTimeString = rs.getString("latest_departure_time");
                LocalTime earliestDepartureTime = LocalTime.parse(departureTimeString);
                LocalTime latestArrivalTime = LocalTime.parse(arrivalTimeString);

                // check if the route is circular
                boolean isCircular = firstFromStopName.equals(lastToStopName);
                System.out.println(firstFromStopName + " " + lastToStopName);
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

            // sort the results by earliest departure time
            results.sort(Comparator.comparing(JourneyRouteInfo::getEarliestDepartureTime));

            return results;
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

        public String getJourneyPatternRef() {
            return journeyPatternRef;
        }

        public String getVehicleJourneyCode() {
            return vehicleJourneyCode;
        }

        public String getFirstFromStop() {
            return firstFromStop;
        }

        public String getFirstFromStopName() {
            return firstFromStopName;
        }

        public String getLastToStop() {
            return lastToStop;
        }

        public String getLastToStopName() {
            return lastToStopName;
        }

        public LocalTime getEarliestDepartureTime() {
            return earliestDepartureTime;
        }

        public LocalTime getLatestArrivalTime() {
            return latestArrivalTime;
        }

        public boolean isCircular() {
            return isCircular;
        }

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