package com.project.busfinder.readDatabase;


import com.project.busfinder.util.readLiveLocation;

import java.io.IOException;
import java.sql.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;


import static com.project.busfinder.util.readLiveLocation.fetchAndProcessResponse;

/**
 *
 * will return a list of all trackable buses that will be used in the combobox
 *
 * This will be done by cross referencing bus routes returned by the API, with the bus
 * route data in the database
 *
 *
 *
 *
 *  find matching Route Names between API and database. From there, match API journey ref with journey pattern
 *
 *
 *  The combobox will have all available routes live or not. some kind of indicator will be added to live routes and
 *  non-live routes so the user knows which routes are active.
 *
 *  once an option has been selected, another combo box will appear, allowing the user to select departure time, with
 *  the top option being Track Live, all other options being the vehicle location at x time.
 */
public class getRoutes {

    public static void main(String[] args) throws IOException, InterruptedException {
        String dbPath = "jdbc:sqlite:data/databases/routes.db";

        try {

            Connection conn = DriverManager.getConnection(dbPath);


            getLiveRoutes(conn);


            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Boolean> getLiveRoutes(Connection conn) throws IOException, InterruptedException {
        ArrayList<AbstractMap.SimpleEntry<String, String>> routes = readLiveLocation.processXmlResponse(fetchAndProcessResponse());
        Map<String, Boolean> routeExistsMap = new HashMap<>();


        try (PreparedStatement pstmt = conn.prepareStatement("SELECT route_id, journey_code FROM journeyCode")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String route_id = rs.getString("route_id");
                routeExistsMap.put(route_id, false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (AbstractMap.SimpleEntry<String, String> entry : routes) {
            String route = entry.getKey();
            String pattern = entry.getValue();



            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM journeyCode WHERE route_id = ? AND journey_code = ?")) {
                pstmt.setString(1, route);
                pstmt.setString(2, pattern);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    // Print or process the matching data
                    String journeyCode = rs.getString("journey_code");
                    String route_id = rs.getString("route_id");

                    //System.out.println("Matching Journey:");
                    //System.out.println("journeyCode: " + journeyCode);
                    //System.out.println("route_id: " + route_id);

                    // Update the map to mark this route as true
                    routeExistsMap.put(route, true);
                } else {
                    //System.out.println("No matching journey found for Route: " + route + ", Pattern: " + pattern);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        System.out.println(routeExistsMap);
        return routeExistsMap;
    }
}
