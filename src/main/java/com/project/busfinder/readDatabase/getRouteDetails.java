package com.project.busfinder.readDatabase;

import com.project.busfinder.GUI.TrackBusPanelController;

import java.sql.*;
import java.time.*;
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
            List<getRouteDetails.JourneyRouteInfo> journeyInfoList = fetcher.getJourneyRouteInfo("10W","Saturday");

            for (getRouteDetails.JourneyRouteInfo info : journeyInfoList) {
                System.out.println(info);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<JourneyRouteInfo> getJourneyRouteInfo(String routeId, String day) throws SQLException {
        // get information about the first stops on the route
        List<JourneyRouteInfo> firstStopInfo = getFirstStopInfo(routeId, day);

        // get information about the last stops on the route
        List<JourneyRouteInfo> lastStopInfo = getLastStopInfo(routeId, day);

        for (int i = 0; i < firstStopInfo.size(); i++) {
            JourneyRouteInfo firstStop = firstStopInfo.get(i);
            JourneyRouteInfo lastStop = lastStopInfo.get(i);

            // set the last stop and latest arrival time for each journey
            firstStop.setLastToStop(lastStop.getLastToStop());
            firstStop.setLatestArrivalDateTime(lastStop.getLatestArrivalDateTime());
        }

        return firstStopInfo;  // return the updated list with first and last stop info
    }

    public List<JourneyRouteInfo> getFirstStopInfo(String routeId, String day) throws SQLException {
        // determine the table name based on the day parameter
        String tableName = getTableNameForDay(day);
        if (tableName == null) {
            throw new IllegalArgumentException("Invalid table name for day: " + day);
        }
        System.out.println("Using table: " + tableName);
        // query to get information about the first stop for each journey
        String query = """
        SELECT 
            journey_pattern_ref,
            vehicle_journey_code,
            from_stop AS first_from_stop,
            MIN(DATE(date / 1000, 'unixepoch') || ' ' || departure_time) AS earliest_departure_datetime
        FROM 
            """ + tableName + """
        WHERE 
            route_id = ?
        GROUP BY 
            journey_pattern_ref,
            vehicle_journey_code;
    """;

        List<JourneyRouteInfo> results = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, routeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // get the details of each journey's first stop
                String journeyPatternRef = rs.getString("journey_pattern_ref");
                String vehicleJourneyCode = rs.getString("vehicle_journey_code");
                String firstFromStop = rs.getString("first_from_stop");
                String earliestDepartureDatetime = rs.getString("earliest_departure_datetime");

                // parse the earliest departure datetime
                LocalDateTime earliestDepartureDateTime = LocalDateTime.parse(earliestDepartureDatetime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // create a new JourneyRouteInfo object with the parsed data
                results.add(new JourneyRouteInfo(journeyPatternRef, vehicleJourneyCode, firstFromStop, earliestDepartureDateTime, null, null));
            }
        }
        return results;  // return the list of journey route info for the first stops
    }

    public List<JourneyRouteInfo> getLastStopInfo(String routeId, String day) throws SQLException {
        // determine the table name based on the day parameter
        String tableName = getTableNameForDay(day);

        // query to get information about the last stop for each journey
        String query = """
        SELECT 
            journey_pattern_ref,
            vehicle_journey_code,
            to_stop AS last_to_stop,
            MAX(DATE(date / 1000, 'unixepoch') || ' ' || departure_time) AS latest_departure_datetime
        FROM 
            """ + tableName + """
        WHERE 
            route_id = ?
        GROUP BY 
            journey_pattern_ref,
            vehicle_journey_code;
    """;

        List<JourneyRouteInfo> results = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, routeId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // get the details of each journey's last stop
                String journeyPatternRef = rs.getString("journey_pattern_ref");
                String vehicleJourneyCode = rs.getString("vehicle_journey_code");
                String lastToStop = rs.getString("last_to_stop");
                String latestDepartureDatetime = rs.getString("latest_departure_datetime");

                // parse the latest departure datetime
                LocalDateTime latestDepartureDateTime = LocalDateTime.parse(latestDepartureDatetime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // create a new JourneyRouteInfo object with the parsed data
                results.add(new JourneyRouteInfo(journeyPatternRef, vehicleJourneyCode, null, null, lastToStop, latestDepartureDateTime));
            }
        }
        return results;  // return the list of journey route info for the last stops
    }

    private String getTableNameForDay(String day) {
        String formattedDay = day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase();
        switch (formattedDay) {
            case "Saturday":
                return "saturday_routes";
            case "Sunday":
                return "sunday_routes";
            case "Monday":
                return "monday_routes";
            case "Tuesday":
                return "tuesday_routes";
            case "Wednesday":
                return "wednesday_routes";
            case "Thursday":
                return "thursday_routes";
            case "Friday":
                return "friday_routes";
            default:
                throw new IllegalArgumentException("Invalid day of the week: " + day);
        }
    }
    public static class JourneyRouteInfo {
        private final String journeyPatternRef;
        private final String vehicleJourneyCode;
        private final String firstFromStop;
        private final LocalDateTime earliestDepartureDateTime;
        private String lastToStop;
        private LocalDateTime latestArrivalDateTime;


        public JourneyRouteInfo(String journeyPatternRef, String vehicleJourneyCode, String firstFromStop, LocalDateTime earliestDepartureDateTime, String lastToStop, LocalDateTime latestArrivalDateTime) {
            this.journeyPatternRef = journeyPatternRef;
            this.vehicleJourneyCode = vehicleJourneyCode;
            this.firstFromStop = firstFromStop;
            this.earliestDepartureDateTime = earliestDepartureDateTime;
            this.lastToStop = lastToStop;
            this.latestArrivalDateTime = latestArrivalDateTime;
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
       // public String getFirstFromStopName() {
        //return firstFromStopName;
        //}

        // getter for the last 'to' stop ID
        public String getLastToStop() {
            return lastToStop;
        }
        public void setLastToStop(String lastToStop) {
            this.lastToStop = lastToStop;
        }
        // getter for the last 'to' stop name
        /// public String getLastToStopName() {
        //return lastToStopName;
    //}

        public LocalDateTime getEarliestDepartureDateTime() {
            return earliestDepartureDateTime;
        }

        public LocalDateTime getLatestArrivalDateTime() {
            return latestArrivalDateTime;
        }
        public void setLatestArrivalDateTime(LocalDateTime latestArrivalDateTime) {
            this.latestArrivalDateTime = latestArrivalDateTime;
        }
        @Override
        public String toString() {
            return String.format("JourneyPatternRef: %s, VehicleJourneyCode: %s,  (%s) ->  (%s), Departure: %s, Arrival: %s",
                    journeyPatternRef, vehicleJourneyCode,  firstFromStop, lastToStop,
                    earliestDepartureDateTime, latestArrivalDateTime);
        }
    }
}