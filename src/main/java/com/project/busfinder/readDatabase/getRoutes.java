package com.project.busfinder.readDatabase;


import com.project.busfinder.Mapping_util.LiveRouteInfo;

import java.io.IOException;
import java.sql.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;


import static com.project.busfinder.util.readLiveLocation.fetchAndProcessResponse;
import static com.project.busfinder.util.readLiveLocation.processXmlResponse;

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
        List<LiveRouteInfo> routeInfoList = processXmlResponse(fetchAndProcessResponse());
        Map<String, Boolean> routeExistsMap = new HashMap<>();
        System.out.println("routes :  " + routeInfoList);

        try (PreparedStatement pstmt = conn.prepareStatement("SELECT route_id, journey_code FROM journeyCode")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String route_id = rs.getString("route_id");
                routeExistsMap.put(route_id, false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (LiveRouteInfo routeInfo : routeInfoList) {
            String route = routeInfo.getLineRef();
            String journeyRef = routeInfo.getJourneyRef();

            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM journeyCode WHERE route_id = ? AND journey_code = ?")) {
                pstmt.setString(1, route);
                pstmt.setString(2, journeyRef);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String journeyCode = rs.getString("journey_code");
                    String route_id = rs.getString("route_id");

                    routeExistsMap.put(route, true); // mark as live if a match is found
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return routeExistsMap;
    }
}
