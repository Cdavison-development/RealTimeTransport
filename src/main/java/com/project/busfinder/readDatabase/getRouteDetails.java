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
            List<getRouteDetails.JourneyRouteInfo> journeyInfoList = fetcher.getJourneyRouteInfo("10A","Saturday");

            for (getRouteDetails.JourneyRouteInfo info : journeyInfoList) {
                System.out.println(info);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<JourneyRouteInfo> getJourneyRouteInfo(String routeId, String day) throws SQLException {
        // determine table based on day parameter
        String tableName;
        switch (day) {
            case "Saturday":
                tableName = "saturday_routes";
                break;
            case "Sunday":
                tableName = "sunday_routes";
                break;
            default:
                tableName = "weekday_routes";
                break;
        }

        // retrieve journey route information, including first and last stops and their times
        String query = """
    WITH FirstStop AS (
        SELECT 
            journey_pattern_ref,
            vehicle_journey_code,
            from_stop AS first_from_stop,
            MIN(departure_time) AS earliest_departure_time
        FROM 
            """ + tableName + """
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
            """ + tableName + """
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

                System.out.println("Raw Departure Time: " + departureTimeString);
                System.out.println("Raw Arrival Time: " + arrivalTimeString);

                LocalTime earliestDepartureTime = LocalTime.parse(departureTimeString);
                LocalTime latestArrivalTime = LocalTime.parse(arrivalTimeString);

                // determine whether the journey crosses midnight
                boolean crossesMidnight = earliestDepartureTime.isAfter(latestArrivalTime);


                if (crossesMidnight) {
                    System.out.println("Journey crosses midnight. Adjusting times.");

                    // adjust earliest tim,e
                    earliestDepartureTime = earliestDepartureTime.minusHours(24);

                    // ensure earliest time is before latest time
                    if (earliestDepartureTime.isAfter(latestArrivalTime)) {
                        System.out.println("Adjusting latest arrival time to reflect the next day.");
                        latestArrivalTime = latestArrivalTime.plusHours(24);
                    }
                }

                System.out.println("Adjusted Earliest Departure Time: " + earliestDepartureTime);
                System.out.println("Adjusted Latest Arrival Time: " + latestArrivalTime);


                System.out.println("Adjusted Earliest Departure Time: " + earliestDepartureTime);
                System.out.println("Adjusted Latest Arrival Time: " + latestArrivalTime);


                if (latestArrivalTime.isBefore(LocalTime.of(1, 0))) {
                    System.out.println("Adjusting latest arrival time to keep within context of previous day.");
                    latestArrivalTime = latestArrivalTime.plusHours(24);
                }


                // create a new JourneyRouteInfo object and add it to the results list
                JourneyRouteInfo info = new JourneyRouteInfo(
                        journeyPatternRef,
                        vehicleJourneyCode,
                        firstFromStop,
                        firstFromStopName,
                        lastToStop,
                        lastToStopName,
                        earliestDepartureTime,
                        latestArrivalTime
                );
                results.add(info);
            }

            // sort results
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
        //private final boolean isCircular;

        // constructor to initialise all fields
        //public JourneyRouteInfo(String journeyPatternRef, String vehicleJourneyCode, String firstFromStop,
                                //String firstFromStopName, String lastToStop, String lastToStopName,
                                //LocalTime earliestDepartureTime, LocalTime latestArrivalTime, boolean isCircular) {

        public JourneyRouteInfo(String journeyPatternRef, String vehicleJourneyCode, String firstFromStop,
                                String firstFromStopName, String lastToStop, String lastToStopName,
                                LocalTime earliestDepartureTime, LocalTime latestArrivalTime) {
            this.journeyPatternRef = journeyPatternRef;
            this.vehicleJourneyCode = vehicleJourneyCode;
            this.firstFromStop = firstFromStop;
            this.firstFromStopName = firstFromStopName;
            this.lastToStop = lastToStop;
            this.lastToStopName = lastToStopName;
            this.earliestDepartureTime = earliestDepartureTime;
            this.latestArrivalTime = latestArrivalTime;
            //this.isCircular = isCircular;
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



        @Override
        public String toString() {
            //String routeDescription = isCircular ? "Circular Route: " : "Route: ";
            String routeDescription = "Route: ";
            return String.format("%sJourneyPatternRef: %s, VehicleJourneyCode: %s, %s (%s) -> %s (%s), Departure: %s, Arrival: %s",
                    routeDescription, journeyPatternRef, vehicleJourneyCode,
                    firstFromStopName, firstFromStop, lastToStopName, lastToStop,
                    earliestDepartureTime, latestArrivalTime);
        }
    }
}